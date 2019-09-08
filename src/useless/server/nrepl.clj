(ns useless.server.nrepl
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.tools.logging :as log]
            [aleph.http :as http]
            [integrant.core :as ig]
            [nrepl.core :as nrepl]
            [nrepl.server :as server]
            [manifold.deferred :as deferred]
            [manifold.stream :as stream])
  (:import (java.net InetAddress ConnectException)))


(defmethod ig/init-key ::server
  [_ {:keys [bind port provided?] :as options}]
  (if (#{"0.0.0.0" "::" "0:0:0:0:0:0:0:0" "[::0]"} bind)
    (throw (ex-info "You do NOT want to bind the nREPL server to all network interfaces." options))
    (if provided?
      {:port port}
      (server/start-server :bind bind :port port))))


(defmethod ig/halt-key! ::server
  [_ server]
  (server/stop-server server))


(defn- connect
  [port]
  (log/log :debug {:event :nrepl/connect :data {:port port}})
  (nrepl/connect :port port))


(defn- origin-host
  [{:keys [headers]}]
  (when-some [origin (get headers "origin")]
    (-> origin
        (io/as-url)
        (.getHost)
        (InetAddress/getByName)
        (.getHostAddress))))


(defn- close-session
  [client-session]
  (log/log :debug {:event :websocket/disconnect})
  (nrepl/message client-session {:op "close"}))


(defn- evaluate-nrepl-message
  [session stream client-input]
  (let [result-seq (nrepl/message session (edn/read-string client-input))
        result (nrepl/combine-responses result-seq)]
    (log/log :debug {:event :nrepl/eval :data {:result result}})
    (stream/put! stream (pr-str result))))


(defmethod ig/init-key ::handler
  [_ {:keys [timeout]}]
  (fn [{{port :port} :params :as request}]
    ;; Only allow WebSocket connections that originate from the loopback
    ;; interface.
    (if (#{"127.0.0.1" "0:0:0:0:0:0:0:1"} (origin-host request))
      (try
        ;; TODO: Could add spec coercion for parameters.
        (let [transport (connect (Integer/parseInt port 10))
              session (nrepl/client-session (nrepl/client transport timeout))]
          (log/log :debug {:event :websocket/connect})
          (deferred/let-flow [stream (http/websocket-connection request)]
            (stream/on-closed stream #(close-session session))
            (stream/connect-via stream #(evaluate-nrepl-message session stream %) stream)))
        (catch ConnectException _
          {:status 502}))
      {:status 403})))
