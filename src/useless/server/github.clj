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


(defn- readme-uri
  [organization repository]
  (format "https://api.github.com/repos/%s/%s/readme" organization repository))


(defn- fetch-readme
  [organization repository]
  (let [content (-> @(http/get (readme-uri organization repository) {:headers headers})
                    :body
                    bytes/to-reader
                    (json/read :key-fn keyword)
                    :content)]
    (bytes/to-string (.decode (Base64/getMimeDecoder) content))))


(defn readme
  [port {{:keys [organization repository]} :path-params}]
  {:status  200
   :body    (html/render port (fetch-readme organization repository))
   :headers {"Content-Type" "text/html"}})
