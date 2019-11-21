(ns useless.client.epoch
  (:require [re-frame.core :as re-frame]
            [useless.client.websocket :as websocket]))


(re-frame/reg-event-db
  :port/set
  (fn [db [_ port]]
    (cond-> db (not (empty? port)) (assoc :prepl/port (js/parseInt port)))))


(re-frame/reg-sub
  :port/get
  (fn [{:keys [prepl/port]} _]
    port))


(re-frame/reg-event-db
  :port/set-editing
  (fn [db [_ value]]
    (assoc db :port/editing-port? value)))


(re-frame/reg-sub
  :port/editing?
  (fn [{:keys [:port/editing-port?]} _]
    editing-port?))


(re-frame/reg-event-fx
  :port/switch
  (fn [{{port :prepl/port :as db} :db} _]
    {:db               db
     :websocket/switch port
     :dispatch         [:port/set-editing false]}))


(re-frame/reg-fx :websocket/switch websocket/switch!)


(re-frame/reg-event-db
  :websocket/update-connection-status
  (fn [db _]
    ;; FIXME: Not quite pure
    (assoc db :websocket/connected? (and @websocket/!stream (websocket/connected? @websocket/!stream)))))


(re-frame/reg-sub
  :websocket/connected?
  (fn [{:keys [websocket/connected?]} _]
    connected?))


(re-frame/reg-fx
  :websocket/evaluate
  (fn [message]
    (websocket/send message)))


(re-frame/reg-event-fx
  :editor/evaluate
  (fn [{db :db} [_ value]]
    {:db                 db
     :websocket/evaluate value}))


(defn- push
  [max-size xs x]
  (cons x (if (< (count xs) max-size) xs (butlast xs))))


(re-frame/reg-event-db
  :editor/add-result
  (fn [db [_ result]]
    (update db :editor/results (partial push 50) result)))


(re-frame/reg-sub
  :editor/results
  (fn [{:keys [editor/results]} _]
    (reverse results)))


(re-frame/reg-event-db
  :editor/clear-results
  (fn [db _]
    (dissoc db :editor/results)))


(re-frame/reg-event-fx
  :app-db/initialize
  (fn [{db :db} [_ {:keys [prepl/port] :as data}]]
    {:db               (merge db data)
     :websocket/switch port}))
