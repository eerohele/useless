(ns useless.server.html
  (:require [selmer.parser :as parser]
            [selmer.util :as util]
            [useless.server.renderer :as renderer]
            [useless.server.markdown]
            [useless.server.asciidoc]))


(defn render
  [prepl-port document]
  (util/without-escaping
    (parser/render-file "html/document.html"
                        {:prepl-port prepl-port
                         :content    (renderer/render document)})))
