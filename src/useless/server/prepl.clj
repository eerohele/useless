(ns useless.server.prepl
  (:require [clojure.core.async :as async]
            [clojure.core.server :as server]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [aleph.http :as http]
            [integrant.core :as ig]
            [manifold.deferred :as deferred]
            [manifold.stream :as stream])
  (:import (java.net InetAddress)
           (java.io PipedReader PipedWriter)
           (java.util UUID)))


(defn- origin-host
  [{:keys [headers]}]
  (when-some [origin (get headers "origin")]
    (-> origin
        (io/as-url)
        (.getHost)
        (InetAddress/getByName)
        (.getHostAddress))))


(defn- start-server
  [name]
  (let [server (server/start-server {:accept 'clojure.core.server/io-prepl
                                     :port   0
                                     :name   name})]
    {:server server
     :port   (.getLocalPort server)}))


(defmulti handle (fn [_ {id :id}] id))


(defmethod handle :eval
  [writer {:keys [data]}]
  (.write writer (str data \newline))
  (.flush writer))


(defmethod handle :default
  [_ message]
  (log/log :error {:event   :websocket/unknown-message
                   :message message}))


(defmethod ig/init-key ::handler
  [_ _]
  (fn [{remote-addr :remote-addr :as request}]
    ;; Only allow WebSocket connections that originate from the loopback
    ;; interface.
    (if (#{"127.0.0.1" "0:0:0:0:0:0:0:1"} (origin-host request))
      (let [server-name (str (UUID/randomUUID))
            {:keys [port]} (start-server server-name)]
        (log/log :info {:event       :websocket/connect
                        :remote-addr remote-addr
                        :port        port})

        (let [writer (PipedWriter.)
              reader (PipedReader. writer)
              in-chan (async/chan 32 (map #(edn/read-string {:default tagged-literal} %)))
              out-chan (async/chan 32 (map pr-str))]

          ;; Kick off an infinite REPL loop in another thread.
          (-> (deferred/future
                (let [out-fn #(async/go (async/>! out-chan {:id :prepl-response :data %}))]
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
                  ;; Close pipe and prepl server when client closes WebSocket
                  ;; connection.
                  (stream/on-closed
                    websocket
                    (fn []
                      (log/log :info {:event       :websocket/disconnect
                                      :remote-addr remote-addr})
                      (.close reader)
                      (when (server/stop-server server-name)
                        (log/log :info {:event       :prepl-server/stopped
                                        :remote-addr remote-addr}))))

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
                    (when-let [message (async/<! in-chan)]
                      (handle writer message)
                      (recur)))

                  ;; Feed every message received from prepl to WebSocket stream.
                  (stream/connect out-chan websocket)

                  ;; Send initial data to WebSocket client.
                  (async/go
                    (async/>! out-chan {:id   :handshake
                                        :data {:prepl-port port}}))

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
                  (server/stop-server server-name)
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