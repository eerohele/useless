(ns useless.server.nrepl-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.edn :as edn]
            [aleph.http :as aleph]
            [aleph.netty :as netty]
            [integrant.core :as ig]
            [manifold.stream :as stream]
            [useless.server.app :as app]
            [useless.server.http :as http]
            [useless.server.nrepl :as nrepl]))


(defn with-test-system
  [keys f]
  (let [system (ig/init (assoc-in app/config [::http/server :port] 0) keys)]
    (try
      (f system)
      (catch Exception ex
        (throw ex))
      (finally
        (ig/halt! system)))))


(defn ports
  [system]
  {:nrepl (get-in system [::nrepl/server :port])
   :http  (netty/port (::http/server system))})


(defn with-stable-keys
  [message & additional-unstable-keys]
  (apply dissoc message (concat [:id :session] additional-unstable-keys)))


(deftest ^:stateful websocket-nrepl-endpoint
  (testing "Sending an nREPL message to the WebSocket endpoint and receiving nREPL result message"
    (with-test-system [::http/server ::nrepl/handler]
      (fn [system]
        (let [{:keys [nrepl http]} (ports system)
              uri (format "ws://[::1]:%s/repl?port=%s" http nrepl)
              connection (aleph/websocket-client uri {:headers {"Origin" (format "http://[::1]:%s" http)}})]

          (stream/put! @connection (pr-str {:op "eval" :code "(+ 1 2)"}))

          (is (= {:ns "user" :value ["3"] :status #{"done"}}
                 (with-stable-keys (edn/read-string @(stream/take! @connection)))))

          (stream/put! @connection (pr-str {:op "eval" :code "(+ 1 2"}))

          (is (= {:err     "Syntax error reading source at (REPL:1:1).
EOF while reading, starting at line 1
"
                  :ex      "class clojure.lang.ExceptionInfo"
                  :root-ex "class java.lang.RuntimeException"
                  :status  #{"done" "eval-error"}}
                 (with-stable-keys (edn/read-string @(stream/take! @connection)))))

          (stream/put! @connection (pr-str {:op "eval" :code "(throw (ex-info \"Boom!\" {}))"}))

          (is (= {:ex      "class clojure.lang.ExceptionInfo"
                  :root-ex "class clojure.lang.ExceptionInfo"
                  :status  #{"done"
                             "eval-error"}}
                 (with-stable-keys (edn/read-string @(stream/take! @connection)) :err))))))))
