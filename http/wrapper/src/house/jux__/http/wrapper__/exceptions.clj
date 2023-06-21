(ns house.jux--.http.wrapper--.exceptions
  (:require [cheshire.core :as json]
            [clojure.pprint :refer [pprint]]
            [house.jux--.exceptions-- :refer [treated-message]]))

(defn- message [e]
  (or (.getMessage e) (-> e .getClass str)))

(defn handle [delegate-handler request]
  (try

    (delegate-handler request)

    (catch Exception e
      (let [message  (message e)
            treated? (= message treated-message)
            message (if treated?
                      (:reason (ex-data e))
                      message)]
        (if treated?
          (println message (:uri request))
          (do (println "Exception handling request:" (:uri request))
              (pprint request)
              (.printStackTrace e)))
        {:status 400
         :body   (json/generate-string {:error message})}))))

(defn wrap-exceptions [delegate-handler]
  (partial handle delegate-handler))
