(ns useless.server.response
  (:require [aleph.http :as http]
            [byte-streams :as bytes]
            [useless.server.html :as html]))


(defn- read-body
  [uri]
  (-> @(http/get (.toString uri) {:headers {"User-Agent" "aleph"}})
      :body
      bytes/to-string))


(defn ok
  [port parse-fn uri]
  {:status  200
   :body    (->> uri (read-body) (parse-fn) (html/render port))
   :headers {"Content-Type" "text/html"}})
