(ns useless.client.app
  (:require [clojure.string :as string]
            [cljs.core.async :as async :include-macros true]
            [hx.react :as hx :include-macros true]
            [hx.hooks :as hooks]
            [useless.client.editor :as editor]
            [useless.client.websocket :as websocket]
            ["react-dom" :as react-dom]))


(defn- default-nrepl-port
  []
  (.getAttribute (.querySelector js/document "meta[name = 'default-nrepl-port']") "content"))


(def !nrepl-port (atom (default-nrepl-port)))


(defn code-blocks
  []
  (array-seq (.querySelectorAll js/document "pre > code")))


(hx/defnc <Port>
  [{:keys [default-port]}]
  (let [[port set-port] (hooks/useState default-port)
        [editing? set-editing] (hooks/useState false)]
    [:div {:class "port"}
     [:p {:class "label"} "Port: "]
     (if editing?
       [:input {:value       port
                :type        "number"
                :min         1024
                :max         65535
                :on-change   #(set-port (when-some [value (.. % -target -value)]
                                          (js/parseInt value)))
                :on-key-down #(case (.-key %)
                                "Esc" (set-editing false)
                                "Enter" (do (reset! !nrepl-port port)
                                            (set-editing false))
                                (constantly nil))}]
       [:p {:on-click #(set-editing true)} port])]))


(hx/defnc <Header>
  [{:keys [default-port]}]
  (let [[connected? update-status] (hooks/useState false)]
    (hooks/useEffect
      (fn []
        (let [interval (js/setInterval
                         #(update-status (fn [_] (websocket/connected?)))
                         1000)]
          #(js/clearInterval interval))))
    [:div {:class (if connected? "connection-status connected"
                                 "connection-status disconnected")}
     [<Port> {:default-port default-port}]
     [:p {:class "message"}
      (if connected?
        "Connection established. All systems nominal."
        "Disconnected.")]]))


(def mode-aliases
  {"language-clojure" "clojure"
   "language-clj"     "clojure"})


(defn mode-name
  [node]
  (let [language (first (array-seq (.-classList node)))]
    (get mode-aliases language language)))


(defn ^:dev/before-load stop!
  []
  (doseq [node (code-blocks)]
    (react-dom/unmountComponentAtNode (.-parentNode node))))


(defn ^:dev/after-load start!
  []
  (async/go
    (reset! websocket/!stream (async/<! (websocket/connect (default-nrepl-port))))

    (add-watch !nrepl-port :nrepl-port-watcher (fn [_ _ _ port] (websocket/switch! port)))

    (doseq [node (code-blocks)]
      (react-dom/render
        (hx/f [editor/<CodeBlock> {:initial-value (string/trim-newline (.-textContent node))
                                   :mode          (mode-name node)}])
        (.-parentNode node)))

    (react-dom/render
      (hx/f [<Header> {:default-port @!nrepl-port}])
      (.querySelector js/document "header"))))


(start!)
