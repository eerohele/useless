(ns useless.server.http
  (:require [clojure.tools.logging :as log]
            [aleph.http :as http]
            [aleph.netty :as netty]
            [integrant.core :as ig])
  (:import (java.io Closeable)
           (java.net InetSocketAddress)))


(defmethod ig/init-key ::server
  [_ {:keys [port handler] :or {port 0}}]
  ;; Bind Netty to loopback interface to prevent connections from elsewhere than
  ;; the machine where the HTTP server was started.
  (let [options {:socket-address (InetSocketAddress. "::1" port)}
        server (http/start-server handler options)]
    (log/log :debug {:event :http/start
                     :data  {:url (format "http://[::1]:%s" (netty/port server))}})
    server))


(defmethod ig/halt-key! ::server
  [_ ^Closeable server]
  (log/log :debug {:event :http/stop})
  (.close server))
