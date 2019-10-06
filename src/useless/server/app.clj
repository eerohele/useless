(ns useless.server.app
  (:require [clojure.java.io :as io]
            [integrant.core :as ig]
            [unilog.config :as unilog]
            [useless.server.handler :as handler]
            [useless.server.http :as http]
            [useless.server.nrepl :as nrepl]))


(unilog/start-logging! {:appenders [{:appender :console
                                     :encoder  :json}]

                        :overrides {"useless.server.http"  :info
                                    "useless.server.nrepl" :info
                                    "org.xnio"             :warn
                                    "org.xnio.nio"         :warn
                                    "org.jboss.threads"    :warn}})


(def default-config
  (ig/read-string (slurp (io/resource "config.edn"))))


(ig/load-namespaces default-config)


(defn start!
  ([]
   (start! default-config))
  ([{{uri        :uri
      nrepl-port :nrepl-port
      http-port  :http-port} :options}]
   (cond-> default-config
           uri (assoc-in [::handler/app :uri] uri)
           http-port (assoc-in [::http/server :port] http-port)
           nrepl-port (assoc-in [::nrepl/server :port] nrepl-port)
           nrepl-port (assoc-in [::nrepl/server :provided?] true)
           true ig/init)))


(defn stop!
  [system]
  (ig/halt! system))


(comment
  (def system (start! {:uri (java.net.URI. "https://gist.github.com/john2x/e1dca953548bfdfb9844")}))
  (def system (start! {:uri (java.net.URI. "https://github.com/anan44/it-starts-with-clojure/blob/master/materials/4-state-in-clojure/2-storing-state-with-atom.md")}))
  (def system (start! {:uri (java.net.URI. "https://raw.githubusercontent.com/anan44/it-starts-with-clojure/master/materials/4-state-in-clojure/1-adding-to-data-structures.md")}))
  (stop! system)
  )