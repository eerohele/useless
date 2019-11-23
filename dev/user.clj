(ns user
  (:require [integrant.repl :as repl :refer [clear go halt prep init reset reset-all]]
            [useless.server.app :as app]))


(set! *warn-on-reflection* true)


(repl/set-prep! (constantly app/default-config))


(comment
  (go)
  (halt)
  (reset))
