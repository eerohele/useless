(ns useless.client.app
  (:require [clojure.string :as string]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [useless.client.editor :as editor]
            [useless.client.websocket :as websocket]
            [useless.client.epoch]))


(defonce websocket-health-check
  (js/setInterval #(re-frame/dispatch [:websocket/update-connection-status]) 1000))


(defn- default-nrepl-port
  []
  (.getAttribute (.querySelector js/document "meta[name = 'default-nrepl-port']") "content"))


(defn <Port>
  []
  (let [port @(re-frame/subscribe [:port/get])]
    [:div.port
     [:p.label "Port: "]
     (if @(re-frame/subscribe [:port/editing?])
       [:input {:default-value port
                :type          "number"
                :min           1024
                :max           65535
                :on-change     #(re-frame/dispatch [:port/set (.. % -target -value)])
                :on-key-down   #(case (.-key %)
                                  "Esc" (re-frame/dispatch [:port/set-editing false])
                                  "Enter" (re-frame/dispatch [:port/switch])
                                  nil)}]
       [:p {:on-click #(re-frame/dispatch [:port/set-editing true])}
        port])]))


(defn <StatusBar>
  []
  (let [connected? @(re-frame/subscribe [:websocket/connected?])]
    [:div.connection-status
     {:class (if connected? "connected" "disconnected")}
     [<Port>]
     [:p.message
      (if connected?
        "Connection established. All systems nominal."
        "Disconnected.")]]))


(defn <Results>
  []
  [:<>
   [:ul
    (for [{:keys [ns value err]} @(re-frame/subscribe [:editor/results])]
      [:li
       [:pre.evaluation-result
        [:code
         [:span.ns ns]
         (when value
           [:span.value (last value)])
         (when err
           [:span.err err])]]])]
   [:button
    {:on-click #(re-frame/dispatch [:editor/clear-results])}
    "✕"]])


(def mode-aliases
  {"language-clojure" "clojure"
   "language-clj"     "clojure"})


(defn mode-name
  [node]
  (let [language (first (array-seq (.-classList node)))]
    (get mode-aliases language language)))


(def header
  (.querySelector js/document "header"))


(defn code-blocks
  []
  (array-seq (.querySelectorAll js/document "pre > code")))


(def aside
  (.querySelector js/document "aside"))


(defn ^:dev/before-load stop!
  []
  (js/clearInterval websocket-health-check)
  (run! #(reagent/unmount-component-at-node (.. % -parentNode -parentNode)) (code-blocks))
  (reagent/unmount-component-at-node header)
  (reagent/unmount-component-at-node aside))


(defn ^:dev/after-load start!
  []
  (re-frame/dispatch-sync [:app-db/initialize {:nrepl/port (default-nrepl-port)}])

  (run!
    (fn [node]
      (reagent/render [editor/<CodeBlock> {:initial-value (string/trim-newline (.-textContent node))
                                           :mode          (mode-name node)}]
                      (.-parentNode node)))
    (code-blocks))

  (reagent/render [<StatusBar>] header)
  (reagent/render [<Results>] aside))


(start!)
