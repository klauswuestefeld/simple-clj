(ns coherence-test.main
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [clojure.edn :as edn]
            [prevayler-clj.prevayler4 :refer [prevayler! handle!]]
            [house.jux--.prevayler4.git-coherence-- :as coherence]
            [clojure.java.io :as io]
            [clojure.tools.namespace.repl :as repl]
            [babashka.cli :as cli]))

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

(defn start-prevayler! [repo-dir git-reset]
  (coherence/start! prevayler!
                    {:business-fn business-fn}
                    {:coherent-mode? true
                     :git-reset? git-reset
                     :src-dir (io/file repo-dir)
                     :refreshable-namespace-prefixes #{'coherence-test}}))

(defn -main [& args]
  (let [{{:keys [port repo-dir git-reset]} :opts} (cli/parse-args args {:coerce {:port :long :git-reset :boolean}})]
    (repl/refresh-all)
    (start-http-server!
     (start-prevayler! repo-dir git-reset)
     port)))
