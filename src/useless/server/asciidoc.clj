(ns useless.server.asciidoc
  (:require [useless.server.renderer :as renderer])
  (:import (org.asciidoctor Asciidoctor$Factory)))


(def ^:private asciidoctor (Asciidoctor$Factory/create))


(derive :type/adoc :type/asciidoc)


(defmethod renderer/render :type/asciidoc
  [{:keys [content]}]
  (.convert asciidoctor content {}))
