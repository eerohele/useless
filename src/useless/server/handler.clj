(ns useless.server.handler
  (:require [clojure.java.io :as io]
            [integrant.core :as ig]
            [reitit.ring :as ring]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [useless.server.html :as html]
            [useless.server.github :as github])
  (:import (java.io FileNotFoundException)))


(defn file
  [port {{path :path} :path-params}]
  (try
    {:status  200
     :body    (html/render port (slurp path))
     :headers {"Content-Type" "text/html"}}
    (catch FileNotFoundException _
      {:status 404})))


(defn resource
  [port {{path :path} :path-params}]
  (if-some [path (io/resource path)]
    {:status  200
     :body    (html/render port (slurp path))
     :headers {"Content-Type" "text/html"}}
    {:status 404}))


(defmethod ig/init-key ::app
  [_ {nrepl-handler :handler/nrepl {port :port} :nrepl/server}]
  (ring/ring-handler
    (ring/router
      [["/repl"
        {:get {:handler    nrepl-handler
               :middleware [[wrap-params]
                            [wrap-keyword-params]]}}]
       ["/d"
        {:middleware [[wrap-defaults site-defaults]]}
        ["/gist/:id"
         {:get {:handler (partial github/gist port)}}]
        ["/readme/github/:organization/:repository"
         {:get {:handler (partial github/readme port)}}]
        ["/file/{*path}"
         {:get {:handler (partial file port)}}]
        ["/classpath/{*path}"
         {:get {:handler (partial resource port)}}]]
       ["/assets/*" (ring/create-resource-handler)]])
    (ring/create-default-handler)
    {:middleware [[wrap-gzip]]}))
