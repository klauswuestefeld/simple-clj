(ns house.jux--.http.pprint--
  (:require
    [clojure.pprint :refer [pprint]]))

(defn- request-summary [request]
  (select-keys request [:uri :query-string :params]))

(defn reply-summary [reply]
  reply)

(defn- handle [delegate-handler request]
  (let [relevant? (-> request :uri (= "/favicon.ico") not)]
    (when relevant?
      (println "-----")
      (pprint (request-summary request)))
    (let [reply (delegate-handler request)]
      (when relevant?
        (println "Reply:")
        (pprint (reply-summary reply)))
      reply)))

(defn wrap-pprint [delegate-handler]
  (partial handle delegate-handler))
