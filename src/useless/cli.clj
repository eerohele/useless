(ns useless.cli
  (:require [clojure.java.browse :as browse]
            [clojure.tools.cli :as cli]
            [useless.server.app :as app])
  (:import (java.net URI))
  (:gen-class))


(def specification
  [["-u" "--uri URL" "URL to Markdown document"
    :parse-fn #(URI. %)]
   ["-p" "--http-port PORT" "HTTP server port number"
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 1024 % 0x10000) "Must be a number between 1024 and 65536"]]
   ["-n" "--nrepl-port PORT" "nREPL server port number"
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 1024 % 0x10000) "Must be a number between 1024 and 65536"]]
   ["-h" "--help"]])


(defn -main
  [& args]
  (let [{{uri :uri} :options :as options} (cli/parse-opts args specification)]
    (app/start! options)
    (if (some? uri)
      (browse/browse-url "http://[::1]:1234")
      (println "Listening on http://[::1]:1234"))))
