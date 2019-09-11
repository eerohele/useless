(ns useless.server.github
  (:require [clojure.data.json :as json]
            [aleph.http :as http]
            [byte-streams :as bytes]
            [useless.server.html :as html])
  (:import (java.util Base64)))


(def ^:private headers {"User-Agent" "aleph"})


(defn- fetch-first-markdown-document-in-gist
  [id]
  ;; Ehhh...
  (-> (http/get (format "https://api.github.com/gists/%s" id) {:headers headers})
      deref
      :body
      bytes/to-reader
      (json/read :key-fn keyword)
      :files
      vals
      (as-> $ (filter (comp #{"text/markdown"} :type) $))
      first
      :content))


(defn gist
  [port {{id :id} :path-params}]
  {:status  200
   :body    (html/render port (fetch-first-markdown-document-in-gist id))
   :headers {"Content-Type" "text/html"}})


(defn get-content-as-string
  [uri]
  (let [content (-> @(http/get uri {:headers headers})
                    :body
                    bytes/to-reader
                    (json/read :key-fn keyword)
                    :content)]
    (bytes/to-string (.decode (Base64/getMimeDecoder) content))))


(defn- fetch-readme
  [owner repo]
  (get-content-as-string (format "https://api.github.com/repos/%s/%s/readme" owner repo)))


(defn readme
  [port {{:keys [owner repo]} :path-params}]
  {:status  200
   :body    (html/render port (fetch-readme owner repo))
   :headers {"Content-Type" "text/html"}})


(defn- fetch-file
  [owner repo path]
  (get-content-as-string (format "https://api.github.com/repos/%s/%s/contents/%s" owner repo path)))


(defn file
  [port {{:keys [owner repo path]} :path-params}]
  {:status  200
   :body    (html/render port (fetch-file owner repo path))
   :headers {"Content-Type" "text/html"}})
