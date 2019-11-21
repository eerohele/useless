(ns useless.server.util
  (:require [clojure.string :as string]))


(defn guess-file-type
  [path]
  (when-let [period-index (string/last-index-of path ".")]
    (keyword "type" (subs path (inc period-index)))))


(comment
  (guess-file-type "foo.md")
  (guess-file-type "foo.adoc")
  (guess-file-type "foo")
  (guess-file-type "foo.markdown.html")
  (guess-file-type "foo.html")
  )