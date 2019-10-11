(ns useless.server.html
  (:require [selmer.parser :as parser]
            [selmer.util :as util])
  (:import (com.vladsch.flexmark.parser Parser)
           (com.vladsch.flexmark.html HtmlRenderer)
           (com.vladsch.flexmark.util.data MutableDataSet)
           (com.vladsch.flexmark.ext.tables TablesExtension)
           (com.vladsch.flexmark.ext.gfm.tasklist TaskListExtension)
           (com.vladsch.flexmark.ext.jekyll.front.matter JekyllFrontMatterExtension)))


(def ^:private flexmark-extensions
  [(TablesExtension/create)
   (TaskListExtension/create)
   (JekyllFrontMatterExtension/create)])


(def ^:private options
  (MutableDataSet.))


(def ^:private parser
  (.build (.extensions (Parser/builder options) flexmark-extensions)))


(def ^:private renderer
  (.build (HtmlRenderer/builder options)))


(defn markdown->html-string
  [input-string]
  (->> input-string (.parse parser) (.render renderer)))


(defn render
  [nrepl-port input-string]
  (util/without-escaping
    (parser/render-file "html/document.html"
                        {:nrepl-port nrepl-port
                         :content    (markdown->html-string input-string)})))
