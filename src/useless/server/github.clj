(ns useless.server.github
  (:require [clojure.data.json :as json]
            [byte-streams :as bytes]
            [useless.server.response :as response]
            [useless.server.util :refer [guess-file-type]])
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
  [ response]
  (let [{:keys [filename content]}
        (-> response
            (json/read-str :key-fn keyword)
            :files
            vals
            (as-> $ (filter (comp #{"Markdown" "AsciiDoc"} :language) $))
            first)]
    {:type    (guess-file-type filename)
     :content content}))


(def ^:private base64-decoder
  (Base64/getMimeDecoder))


(defn- parse-file
  [response]
  (let [{filename :name content :content} (json/read-str response :key-fn keyword)]
    {:type    (guess-file-type filename)
     :content (->> content (.decode base64-decoder) bytes/to-string)}))


(defn gist
  [{{id :id} :path-params}]
  (response/ok parse-gist (gist-uri id)))


(defn readme
  [{{:keys [owner repo]} :path-params}]
  (response/ok parse-file (readme-uri owner repo)))


(defn file
  [{{:keys [owner repo path]} :path-params}]
  (response/ok parse-file (file-uri owner repo path)))
