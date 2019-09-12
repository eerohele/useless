(ns user
  (:require [clojure.java.io :as io]
            [integrant.core :as ig]
            [integrant.repl :as repl :refer [clear go halt prep init reset reset-all]]
            [useless.server.app :as app]))


(set! *warn-on-reflection* true)


(repl/set-prep! (constantly app/config))


(comment
  (go)
  (halt)
  (reset))
