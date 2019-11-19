(ns useless.client.websocket
  (:require [cljs.core.async :as async :include-macros true]
            [cljs.reader :as edn]
            [haslett.client :as client]
            [re-frame.core :as re-frame]))


(defonce !stream (atom nil))


(defn endpoint
  [port]
  (str "ws://" (.-host (.-location js/window)) "/repl?port=" port))


(defn connect
  [port]
  (client/connect (endpoint port) {:chan (async/chan 32)}))


(defn close!
  []
  (when-some [stream @!stream]
    (client/close stream)))


(def connected? client/connected?)


(defn send
  [message]
  (let [{:keys [sink]} @!stream]
    (async/go
      (.debug js/console {:event :message/->server :message message})
      (async/>! sink message))))


(defn switch!
  [port]
  (async/go
    (when @!stream (client/close @!stream))
    (let [{:keys [source] :as conn} (async/<! (connect port))]
      (reset! !stream conn)

      (async/go-loop
        []
        (when-some [message (async/<! source)]
          (.debug js/console {:event :message/<-server :message message})
          (re-frame/dispatch [:editor/add-result (edn/read-string {:default tagged-literal} message)])
          (recur))))))
