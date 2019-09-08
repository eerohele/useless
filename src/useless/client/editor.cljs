(ns useless.client.editor
  (:require [hx.react :as hx :include-macros true]
            [hx.hooks :as hooks]
            [useless.client.websocket :as websocket]
            ["codemirror" :as CodeMirror]
            ["codemirror/mode/clojure/clojure"]
            ["codemirror/addon/edit/matchbrackets"]
            ["parinfer-codemirror" :as parinfer-codemirror]
            ["react" :as react]))


(defn string-between-matching-brackets
  [editor matching-brackets]
  ;; This is ugly as shit.
  (if (.-forward matching-brackets)
    (do (set! (.-ch (.-to matching-brackets)) (inc (.-ch (.-to matching-brackets))))
        (.getRange editor (.-from matching-brackets) (.-to matching-brackets)))
    (do (set! (.-ch (.-from matching-brackets)) (inc (.-ch (.-from matching-brackets))))
        (.getRange editor (.-to matching-brackets) (.-from matching-brackets)))))


(defn code-string
  [editor]
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


(hx/defnc <CodeBlock>
  [{:keys [initial-value mode]}]
  (let [mount-point (react/useRef nil)
        [result update-result] (hooks/useState nil)
        editable? (= mode "clojure")]
    (hooks/useEffect
      (fn []
        ;; TODO: CodeMirror initialization doesn't support hot-reloading.
        ;;
        ;; Would be nice if it did, but I'm not sure how to make that work.
        (let [options #js {:value          initial-value
                           :lineNumbers    false
                           :viewportMargin js/Infinity
                           :matchBrackets  true
                           :mode           mode
                           :readOnly       (not editable?)
                           :theme          "dracula"}
              editor (CodeMirror. (.-current mount-point) options)]
          (when editable?
            (let [event-handler #(websocket/evaluate update-result (code-string %))]
              (.setOption editor "extraKeys" #js {:Cmd-Enter  event-handler
                                                  :Ctrl-Enter event-handler}))
            (parinfer-codemirror/init editor))))
      [])
    [:div {:class "codeblock"}
     [:div {:class "editor" :ref mount-point}]
     [:div {:class "evaluation-result"}
      [:code
       [:span {:class "ns"} (:ns result)]
       (when-some [values (:value result)]
         [:span {:class "value"} (last values)])
       (when-some [err (:err result)]
         [:span {:class "err"} err])]]]))
