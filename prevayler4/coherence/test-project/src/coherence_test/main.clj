(ns coherence-test.main
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [clojure.edn :as edn]
            [prevayler-clj.prevayler4 :refer [prevayler! handle!]]
            [house.jux--.prevayler4.coherence-- :as coherence]
            [clojure.java.io :as io]
            [clojure.tools.namespace.repl :as repl]))

(defn handler [prevayler]
  (fn [{:keys [uri body]}]
    (case uri
      "/command" (do
                   (handle! prevayler (edn/read (java.io.PushbackReader. (io/reader body))))
                   {:status 200})
      "/state" {:status 200
                :body (pr-str @prevayler)})))

(defn start-http-server! [prevayler port]
  (run-jetty (handler prevayler) {:port port :join? false}))

(defn business-fn [state {:keys [fn-sym args]} _]
  (let [fun (find-var fn-sym)]
    (apply fun state args)))

(defn start-prevayler! [repo-dir]
  (coherence/start! prevayler!
                    {:business-fn business-fn}
                    {:coherent-mode? true
                     :src-dir (io/file repo-dir)
                     :refreshable-namespaces #{'coherence-test}}))

(defn -main [& [port repo-dir]]
  (repl/refresh-all)
  (start-http-server!
   (start-prevayler! repo-dir)
   (Integer/parseInt port)))
