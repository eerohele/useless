(ns useless.server.prepl
  (:require [clojure.core.async :as async]
            [clojure.core.server :as server]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [aleph.http :as http]
            [integrant.core :as ig]
            [manifold.deferred :as deferred]
            [manifold.stream :as stream])
  (:import (java.net InetAddress)
           (java.io PipedReader PipedWriter)))


(defmethod ig/init-key ::server
  [_ {:keys [bind port provided? server-name] :or {server-name "jvm"} :as options}]
  (if (#{"0.0.0.0" "::" "0:0:0:0:0:0:0:0" "[::0]"} bind)
    (throw (ex-info "You do NOT want to bind the pREPL server to all network interfaces." options))
    (let [server (server/start-server {:accept 'clojure.core.server/io-prepl
                                       :port   0
                                       :name   server-name})]
      {:server server
       :port   (if provided?
                 port
                 (.getLocalPort server))})))


(defmethod ig/halt-key! ::server
  [_ {:keys [server-name]}]
  (server/stop-server server-name))


(defn- origin-host
  [{:keys [headers]}]
  (when-some [origin (get headers "origin")]
    (-> origin
        (io/as-url)
        (.getHost)
        (InetAddress/getByName)
        (.getHostAddress))))


(defmethod ig/init-key ::handler
  [_ _]
  (fn [{{port :port} :params remote-addr :remote-addr :as request}]
    ;; Only allow WebSocket connections that originate from the loopback
    ;; interface.
    (if (#{"127.0.0.1" "0:0:0:0:0:0:0:1"} (origin-host request))
      (do
        (log/log :info {:event       :websocket/connect
                        :remote-addr remote-addr
                        :port        port})

        (let [writer (PipedWriter.)
              reader (PipedReader. writer)
              in-chan (async/chan 32)
              out-chan (async/chan 32 (map pr-str))]

          ;; Kick off an infinite REPL loop in another thread.
          (-> (deferred/future
                (let [out-fn #(async/>!! out-chan %)]
                  (server/remote-prepl "localhost" port reader out-fn
                                       :valf #(binding [*default-data-reader-fn* tagged-literal]
                                                (read-string %)))))

              ;; This is a hack, and there must be a better way of doing this.
              ;;
              ;; Because `remote-prepl` blocks, we can't use `chain` or
              ;; `let-flow`. Furthermore, getting our hands on any exceptions in
              ;; a future requires dereferencing it, which we can't do without
              ;; blocking the whole process.
              ;;
              ;; Therefore, we're saying here that if dereferencing the future
              ;; that contains `remote-prepl` fails within the allotted time,
              ;; we know it threw an exception that we want to propagate down
              ;; the chain.
              ;;
              ;; In other words, timeout is the happy case here.
              (deferred/timeout! 500 :repl/init-complete)

              (deferred/chain
                ;; We've established REPL connectivity, but we don't really
                ;; need to hold a reference to it. We can move on to the
                ;; WebSocket connection.
                (fn [_] (http/websocket-connection request))

                (fn [websocket]
                  ;; Close pipe when client closes WebSocket connection.
                  (stream/on-closed
                    websocket
                    (fn []
                      (log/log :info {:event       :websocket/disconnect
                                      :remote-addr remote-addr})
                      (.close reader)))

                  ;; Proxy every received WebSocket message into a core.async
                  ;; channel that we can use in a go-loop.
                  (stream/connect websocket in-chan)

                  ;; Write every inbound WebSocket message into the pipe.
                  ;;
                  ;; Whatever we write into the pipe is read by the reader
                  ;; we pass to the prepl client.
                  ;;
                  ;; I'm using a go-loop over Manifold's stream consumption
                  ;; interfaces because both `consume` and `consume-async`
                  ;; cause spurious IO stream closures for reason's I can't
                  ;; even begin to understand. As always, Stupid User Error is
                  ;; the usual suspect.
                  (async/go-loop
                    []
                    (when-let [input (async/<! in-chan)]
                      (.write writer (str input \newline))
                      (.flush writer)
                      (recur)))

                  ;; Feed every message received from prepl to WebSocket stream.
                  (stream/connect out-chan websocket)

                  (log/log :info {:event       :websocket/connection-established
                                  :remote-addr remote-addr
                                  :port        port})))

              (deferred/catch Throwable
                (fn [throwable]
                  (log/log :error {:event     :exception
                                   :at        :establish-websocket-connection
                                   :exception (Throwable->map throwable)
                                   :port      port})
                  (.close reader)
                  {:status 400})))))
      {:status 403})))


(comment
  (server/start-server {:accept 'clojure.core.server/io-prepl
                        :port   31337
                        :name   "test"})

  (do
    (def input-stream (stream/stream))
    (def writer (PipedWriter.))
    (def reader (PipedReader. writer))

    (defn write
      [message]
      (.write writer message)
      (.flush writer)
      true)

    (deferred/future
      (server/remote-prepl "localhost" 31337 reader println))

    ;; Causes the java.io stream to close under even moderately high load.
    (stream/consume-async #(deferred/future (write %)) input-stream)
    )

  (stream/put! input-stream "+")
  (stream/put! input-stream "+\n")
  (stream/put! input-stream "(+ 1 2)")

  (pmap #(stream/put! input-stream (format "(do (Thread/sleep 50) (* %d %d))" % %))
        (range 1000))

  (.close reader)
  (.close writer)
  )