(ns useless.client.websocket
  (:require [clojure.edn :as edn]
            [cljs.core.async :as async :include-macros true]
            [haslett.client :as client]))


(def !stream (atom nil))


(defn message
  [value]
  (pr-str {:op "eval" :code value}))


(defn evaluate
  [update-fn value]
  (let [{:keys [sink source]} (deref !stream)]
    (async/go
     (async/>! sink (message value))
     (let [result-message (edn/read-string (async/<! source))]
       (update-fn (fn [_] result-message))))))


(defn endpoint
  [port]
  (str "ws://" (.-host (.-location js/window)) "/repl?port=" port))


(defn connect
  [port]
  (client/connect (endpoint port)))


(defn switch!
  [port]
  (async/go
    (when (some? @!stream)
      (client/close @!stream))
    (reset! !stream (async/<! (connect port)))))


(defn connected?
  []
  (client/connected? @!stream))