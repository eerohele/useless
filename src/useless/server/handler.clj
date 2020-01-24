(ns useless.server.handler
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [integrant.core :as ig]
            [reitit.ring :as ring]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.response :refer [redirect]]
            [useless.server.html :as html]
            [useless.server.github :as github]
            [useless.server.response :as response]
            [useless.server.util :refer [guess-file-type]])
  (:import (java.io FileNotFoundException)))


(defn file
  [{{path :path} :path-params}]
  (try
    {:status  200
     :body    (html/render {:type    (guess-file-type path)
                            :content (slurp path)})
     :headers {"Content-Type" "text/html"}}
    (catch FileNotFoundException _
      {:status 404})))


(defn resource
  [{{path :path} :path-params}]
  (if-some [path (io/resource path)]
    {:status  200
     :body    (html/render {:type    (guess-file-type (str path))
                            :content (slurp path)})
     :headers {"Content-Type" "text/html"}}
    {:status 404}))


(defn ^:private split-on-forward-slash
  [uri]
  (string/split uri #"/"))


(defmulti redirection-to
  (fn [uri] (.getHost uri)))


(defmethod redirection-to "github.com"
  [uri]
  (let [[_ owner repo _ _ & rest] (split-on-forward-slash (.getPath uri))
        path (string/join \/ rest)]
    (redirect (format "/github/file/%s/%s/%s" owner repo path))))


(defmethod redirection-to "gist.github.com"
  [uri]
  (let [id (last (split-on-forward-slash (.getPath uri)))]
    (redirect (format "/gist/%s" id))))


(defmethod redirection-to :default
  [uri]
  (redirect (format "/uri?uri=%s" (.toASCIIString uri))))


(defmethod ig/init-key ::app
  [_ {uri :uri prepl-handler :handler/prepl}]
  (ring/ring-handler
    (ring/router
      [["/"
        {:get {:handler    (fn [_]
                             (if uri (redirection-to uri) {:status 404}))
               :middleware [[wrap-defaults site-defaults]]}}]
       ["/repl"
        {:get {:handler    prepl-handler
               :middleware [[wrap-params]
                            [wrap-keyword-params]]}}]
       ["/uri"
        {:get {:handler    (fn [{{query-uri :uri} :query-params}]
                             (response/ok identity (or query-uri uri)))
               :middleware [[wrap-defaults site-defaults]]}}]
       ["/gist/:id"
        {:get        {:handler github/gist}
         :middleware [[wrap-defaults site-defaults]]}]
       ["/github/readme/:owner/:repo"
        {:get        {:handler github/readme}
         :middleware [[wrap-defaults site-defaults]]}]
       ["/github/file/:owner/:repo/{*path}"
        {:get        {:handler github/file}
         :middleware [[wrap-defaults site-defaults]]}]
       ["/file/{*path}"
        {:get        {:handler file}
         :middleware [[wrap-defaults site-defaults]]}]
       ["/classpath/{*path}"
        {:get        {:handler resource}
         :middleware [[wrap-defaults site-defaults]]}]
       ["/assets/*" (ring/create-resource-handler)]])
    (ring/create-default-handler)
    {:middleware [[wrap-gzip]]}))
