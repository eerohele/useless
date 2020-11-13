(ns user
  (:require
   [integrant.core :as ig]
   [integrant.repl :as repl :refer [clear go halt prep init reset reset-all]]
   [figwheel.main.api :as figwheel]
   [useless.server.app :as app]))


(set! *warn-on-reflection* true)


(repl/set-prep! (constantly app/default-config))


(defmethod ig/init-key ::figwheel
  [_ {:keys [id]}]
  (figwheel/start {:mode :serve} id)
  id)


(defmethod ig/halt-key! ::figwheel
  [_ id]
  (figwheel/stop id))


(comment
  (go)
  (halt)
  (reset))
