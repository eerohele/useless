(ns useless.client.app
  (:require [clojure.string :as string]
            [fipp.edn :as fipp]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [useless.client.editor :as editor]
            [useless.client.epoch]
            [useless.client.websocket :as websocket]))


(defonce websocket-health-check
  (js/setInterval #(re-frame/dispatch [:websocket/update-connection-status]) 1000))


(defn- default-prepl-port
  []
  (.getAttribute (.querySelector js/document "meta[name = 'default-prepl-port']") "content"))


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


(defmulti render-result :tag)


(defmethod render-result :ret
  [{:keys [val exception]}]
  [:span {:class (cond exception :err :else :value)}
   (with-out-str (fipp/pprint val))])


(defmethod render-result :out
  [{:keys [val]}]
  [:span.out
   (string/trim (with-out-str (println val)))])


(defn <Result>
  [{:keys [ns] :as result}]
  [:li
   [:pre.evaluation-result
    [:code
     [:span.ns ns]
     (when (contains? result :val)
       (render-result result))]]])


(defn <ResultList>
  []
  (reagent/create-class
    {:component-did-update
     #(let [el (reagent/dom-node %)]
       (set! (.-scrollTop el) (- (.-scrollHeight el) (.-clientHeight el))))

     :render
     (fn [_]
       (into [:ul]
             (map <Result> @(re-frame/subscribe [:editor/results]))))}))


(defn <Results>
  []
  [:div
   [<ResultList>]
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
  (websocket/close!)
  (js/clearInterval websocket-health-check)
  (run! #(reagent/unmount-component-at-node (.. % -parentNode -parentNode)) (code-blocks))
  (reagent/unmount-component-at-node header)
  (reagent/unmount-component-at-node aside))


(defn ^:dev/after-load start!
  []
  (re-frame/dispatch-sync [:app-db/initialize {:prepl/port (default-prepl-port)}])

  (run!
    (fn [node]
      (reagent/render [editor/<CodeBlock> {:initial-value (string/trim-newline (.-textContent node))
                                           :mode          (mode-name node)}]
                      (.-parentNode node)))
    (code-blocks))

  (reagent/render [<StatusBar>] header)
  (reagent/render [<Results>] aside)

  ;; For some reason, the page tends to open up scrolled halfway down to the
  ;; middle of the page.
  ;;
  ;; This here's a hack that ensures that the page is scrolled up to the top of
  ;; the page.
  (.scrollTo js/window 0 0))


(start!)
