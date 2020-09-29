(ns useless.client.editor
  (:require [reagent.core :as reagent]
            [reagent.dom :as dom]
            [re-frame.core :as re-frame]
            ["codemirror" :as CodeMirror]
            ["codemirror/mode/clojure/clojure"]
            ["codemirror/addon/edit/matchbrackets"]
            ["parinfer-codemirror" :as parinfer-codemirror]))


(defn string-between-matching-brackets
  [editor matching-brackets]
  ;; This is ugly as shit.
  (if (.-forward matching-brackets)
    (do (set! (.-ch (.-to matching-brackets)) (inc (.-ch (.-to matching-brackets))))
        (.getRange editor (.-from matching-brackets) (.-to matching-brackets)))
    (do (set! (.-ch (.-from matching-brackets)) (inc (.-ch (.-from matching-brackets))))
        (.getRange editor (.-to matching-brackets) (.-from matching-brackets)))))


(defn code-string
  [^js editor]
  (let [selection (.getSelection editor)
        matching-brackets (.findMatchingBracket editor (.getCursor editor))]
    (cond
      ;; If the user has selected text, evaluate the selection.
      (not (empty? selection))
      selection

      ;; If the cursor is at a bracket and there's a matching bracket, evaluate
      ;; the form delimited by those brackets.
      (and matching-brackets (.-match matching-brackets))
      (string-between-matching-brackets editor matching-brackets)

      ;; Otherwise, evaluate every form in the editor.
      :else
      (.getValue editor))))


(defn <CodeBlock>
  [{:keys [mode initial-value]}]
  (reagent/create-class
    {:component-did-mount (fn [this]
                            (let [read-only? (not= mode "clojure")
                                  options #js {:value          initial-value
                                               :lineNumbers    false
                                               :matchBrackets  true
                                               :mode           mode
                                               :readOnly       read-only?
                                               :theme          "dracula"}
                                  editor (CodeMirror. (dom/dom-node this) options)]
                              (when-not read-only?
                                (let [event-handler #(re-frame/dispatch [:editor/evaluate (code-string %)])]
                                  (.setOption editor "extraKeys" #js {:Cmd-Enter  event-handler
                                                                      :Ctrl-Enter event-handler}))
                                ;; https://github.com/shaunlebron/parinfer-codemirror/issues/11
                                (parinfer-codemirror/init editor))))

     :reagent-render      (fn [_ _ _]
                            [:div.codeblock])}))
