(ns useless.server.response
  (:require [clojure.core.cache.wrapped :as cache]
            [aleph.http :as http]
            [byte-streams :as bytes]
            [manifold.deferred :as deferred]
            [useless.server.html :as html]))


(def ^:private cache
  (cache/ttl-cache-factory {} :ttl 3600))


(defn- read-body
  [uri]
  @(deferred/chain (http/get uri {:headers {"User-Agent" "aleph"}})
                   :body
                   bytes/to-string))


(defn ok
  [parse-fn uri]
  {:status  200
   :body    (->> (cache/lookup-or-miss cache (.toString uri) read-body)
                 (parse-fn)
                 (html/render))
   :headers {"Content-Type" "text/html"}})
