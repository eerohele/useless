(ns useless.server.html
  (:require [selmer.parser :as parser]
            [selmer.util :as util]
            [useless.server.markdown :as markdown]))


(defn render
  [prepl-port input-string]
  (util/without-escaping
    (parser/render-file "html/document.html"
                        {:prepl-port prepl-port
                         :content    (markdown/render input-string)})))
