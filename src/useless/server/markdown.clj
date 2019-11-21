(ns useless.server.markdown
  (:import (java.util ArrayList)
           (com.vladsch.flexmark.parser Parser)
           (com.vladsch.flexmark.html HtmlRenderer)
           (com.vladsch.flexmark.util.data MutableDataSet)
           (com.vladsch.flexmark.ext.tables TablesExtension)
           (com.vladsch.flexmark.ext.gfm.tasklist TaskListExtension)
           (com.vladsch.flexmark.ext.jekyll.front.matter JekyllFrontMatterExtension)))


(def options
  (doto (MutableDataSet.)
    (.set HtmlRenderer/RENDER_HEADER_ID true)
    (.set Parser/EXTENSIONS
          (ArrayList. [(TablesExtension/create)
                       (TaskListExtension/create)
                       (JekyllFrontMatterExtension/create)]))))


(def ^:private parser
  (.build (Parser/builder options)))


(def ^:private renderer
  (.build (HtmlRenderer/builder options)))


(defn render
  [input-string]
  (->> input-string (.parse parser) (.render renderer)))
