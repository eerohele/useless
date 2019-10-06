(ns useless.server.github
  (:require [clojure.data.json :as json]
            [byte-streams :as bytes]
            [useless.server.response :as response])
  (:import (java.util Base64)))


(defn gist-uri
  [id]
  (format "https://api.github.com/gists/%s" id))


(defn readme-uri
  [owner repo]
  (format "https://api.github.com/repos/%s/%s/readme" owner repo))


(defn file-uri
  [owner repo path]
  (format "https://api.github.com/repos/%s/%s/contents/%s" owner repo path))


(defn- parse-gist
  [response]
  (-> response
      (json/read-str :key-fn keyword)
      :files
      vals
      (as-> $ (filter (comp #{"text/markdown"} :type) $))
      first
      :content))


(def ^:private base64-decoder
  (Base64/getMimeDecoder))


(defn- parse-file
  [file]
  (->> (json/read-str file :key-fn keyword)
       :content
       (.decode base64-decoder)
       (bytes/to-string)))


(defn gist
  [port {{id :id} :path-params}]
  (response/ok port parse-gist (gist-uri id)))


(defn readme
  [port {{:keys [owner repo]} :path-params}]
  (response/ok port parse-file (readme-uri owner repo)))


(defn file
  [port {{:keys [owner repo path]} :path-params}]
  (response/ok port parse-file (file-uri owner repo path)))
