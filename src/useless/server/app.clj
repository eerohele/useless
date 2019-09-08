(ns useless.server.app
  (:require [clojure.java.io :as io]
            [integrant.core :as ig]
            [unilog.config :as unilog]))


(unilog/start-logging! {:appenders [{:appender :console
                                     :encoder  :json}]

                        :overrides {"useless.server.http"  :info
                                    "useless.server.nrepl" :info
                                    "org.xnio"             :warn
                                    "org.xnio.nio"         :warn
                                    "org.jboss.threads"    :warn}})


(def config
  (ig/read-string (slurp (io/resource "config.edn"))))


(ig/load-namespaces config)
