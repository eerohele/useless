(ns useless.server.html
  (:require [clojure.java.io :as io]
            [selmer.parser :as parser]
            [selmer.util :as util])
  (:import (com.vladsch.flexmark.parser Parser)
           (com.vladsch.flexmark.html HtmlRenderer)
           (com.vladsch.flexmark.util.data MutableDataSet)
           (com.vladsch.flexmark.ext.tables TablesExtension)
           (com.vladsch.flexmark.ext.gfm.tasklist TaskListExtension)))


(defn markdown->html-string
  [input-string]
  (let [options (MutableDataSet.)
        parser (.build (.extensions (Parser/builder options) [(TablesExtension/create) (TaskListExtension/create)]))
        renderer (.build (HtmlRenderer/builder options))
        document (.parse parser input-string)]
    (.render renderer document)))


(defn render
  [nrepl-port input-string]
  (util/without-escaping
    (parser/render-file "html/document.html"
                        {:nrepl-port nrepl-port
                         :content    (markdown->html-string input-string)})))
