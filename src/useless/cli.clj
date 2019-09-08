(ns useless.cli
  (:require [clojure.tools.cli :as cli]
            [useless.server.app :as app]
            [integrant.core :as ig]
            [useless.server.http :as http]
            [useless.server.nrepl :as nrepl])
  (:gen-class))


(def specification
  [["-p" "--http-port PORT" "HTTP server port number"
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 1024 % 0x10000) "Must be a number between 1024 and 65536"]]
   ["-n" "--nrepl-port PORT" "nREPL server port number"
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 1024 % 0x10000) "Must be a number between 1024 and 65536"]]
   ["-h" "--help"]])


(defn -main
  [& args]
  (let [{{nrepl-port :nrepl-port http-port :http-port} :options} (cli/parse-opts args specification)]
    (ig/init (cond-> app/config
                     http-port (assoc-in [::http/server :port] http-port)
                     nrepl-port (assoc-in [::nrepl/server :port] nrepl-port)
                     nrepl-port (assoc-in [::nrepl/server :provided?] true)))
    (println "Listening on http://[::1]:1234")))
