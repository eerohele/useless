(ns useless.client.websocket
  (:require [cljs.core.async :as async :include-macros true]
            [cljs.reader :as edn]
            [haslett.client :as client]
            [re-frame.core :as re-frame]))


(defonce !stream (atom nil))


(defn endpoint
  []
  (str "ws://" (.-host (.-location js/window)) "/repl"))


(defn connect
  []
  (client/connect (endpoint) {:chan (async/chan 32)}))


(defn close!
  []
  (when-some [stream @!stream]
    (client/close stream)))


(defn send
  [message]
  (let [{:keys [sink]} @!stream]
    (async/go
      (.debug js/console {:event :message/->server :message message})
      (async/>! sink message))))


(defmulti handle :id)


(defmethod handle :handshake
  [{{port :prepl-port} :data}]
  (re-frame/dispatch [:port/set port]))


(defmethod handle :prepl-response
  [{response :data}]
  (re-frame/dispatch [:editor/add-result response]))


(defn switch!
  []
  (async/go
    (when @!stream (client/close @!stream))
    (let [{:keys [source] :as conn} (async/<! (connect))]
      (reset! !stream conn)

      (async/go-loop
        []
        (when-some [message (async/<! source)]
          (.debug js/console {:event :message/<-server :message message})
          (handle (edn/read-string {:default tagged-literal} message))
          (recur))))))


(defn connected?
  []
  (and @!stream (client/connected? @!stream)))
