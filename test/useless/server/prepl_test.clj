(ns useless.server.prepl-test
  (:require [clojure.core.match :refer [match]]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.edn :as edn]
            [aleph.http :as aleph]
            [aleph.netty :as netty]
            [integrant.core :as ig]
            [manifold.deferred :as d]
            [manifold.stream :as stream]
            [useless.server.app :as app]
            [useless.server.http :as http]
            [useless.server.prepl :as prepl]))


(defn with-test-system
  [keys f]
  (let [system (ig/init (assoc-in app/default-config [::http/server :port] 0) keys)]
    (try
      (f system)
      (catch Exception ex
        (throw ex))
      (finally
        (ig/halt! system)))))


(defn ports
  [system]
  {:prepl (get-in system [::prepl/server :port])
   :http  (netty/port (::http/server system))})


(defn- take-val!
  [stream]
  (edn/read-string {:default tagged-literal} @(stream/take! stream)))


(defn- websocket-connection
  [system]
  (let [{:keys [prepl http]} (ports system)
        uri (format "ws://[::1]:%s/repl?port=%s" http prepl)]
    @(aleph/websocket-client uri {:headers {"Origin" (format "http://[::1]:%s" http)}})))


(deftest ^:stateful websocket-prepl-endpoint
  (with-test-system [::http/server ::prepl/handler]
    (fn [system]
      @(d/let-flow [websocket (websocket-connection system)]
         (stream/put! websocket "(+ 1 2)")

         (is (match (take-val! websocket)
                    {:tag :ret :val 3 :ns "user" :form "(+ 1 2)" :ms _} true
                    :else false))

         (stream/put! websocket "(throw (ex-info \"Boom!\" {}))")

         (is (match (take-val! websocket)
                    {:exception true
                     :form      "(throw (ex-info \"Boom!\" {}))"
                     :ns        "user"
                     :tag       :ret
                     :val       {:cause "Boom!"
                                 :data  {}
                                 :phase :execution
                                 :trace _
                                 :via   _}} true
                    :else false))

         (stream/put! websocket "+")

         (let [{:keys [val] :as message} (take-val! websocket)]
           (is (tagged-literal? val))

           (is (match message {:form "+"
                               :ms   _
                               :ns   "user"
                               :tag  :ret
                               :val  _} true
                      :else false)))))))


(deftest ^:stateful ^:slow stress-test
  (with-test-system [::http/server ::prepl/handler]
    (fn [system]
      (let [num 1000]
        @(d/let-flow [websocket (websocket-connection system)]
           (letfn [(put-test-val!
                     [x]
                     (deref (stream/put! websocket (format "(do (Thread/sleep 50) (* %d %d))" x x))))]
             (doall
               (pmap put-test-val! (range num))))

           (is (= num (count (stream/stream->seq websocket 1000)))))))))
