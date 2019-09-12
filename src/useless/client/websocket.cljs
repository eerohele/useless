(ns useless.client.websocket
  (:require [cljs.core.async :as async :include-macros true]
            [haslett.client :as client]))


(defonce !stream (atom nil))


(defn endpoint
  [port]
  (str "ws://" (.-host (.-location js/window)) "/repl?port=" port))


(defn connect
  [port]
  (client/connect (endpoint port)))


(def connected? client/connected?)


(defn switch!
  [port]
  (async/go
    (when @!stream (client/close @!stream))
    (reset! !stream (async/<! (connect port)))))
