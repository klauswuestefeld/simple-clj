(ns house.jux--.http.wrapper--.pprint
  (:require
    [clojure.pprint :refer [pprint]]))

(defn- request-summary [request]
  (-> request 
      (update :params dissoc "captcha-token" "text")
      (select-keys [:uri :query-string :params])))

(defn- handle [delegate-handler request]
  (let [relevant? (-> request :request-method (not= :options))]
    (when relevant?
      (println "-----")
      (pprint (request-summary request)))
    (delegate-handler request)))

(defn wrap-pprint [delegate-handler]
  (partial handle delegate-handler))
