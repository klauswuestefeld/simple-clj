(ns house.jux--.http.exceptions--
  (:require
   [clojure.stacktrace :as stacktrace]
   [house.jux--.exceptions-- :refer [message expected?]]))

(defn- ->body [e message expected?]
  (if expected?
    {:error message, :expected true}
    {:error message, :stacktrace (with-out-str (stacktrace/print-cause-trace e))}))


(defn handle [delegate-handler request]
  (try

    (delegate-handler request)

    (catch Exception e
      (let [message   (message e)
            expected? (expected? e)]
        (if expected?
          (println message (:uri request))
          (do (println "Exception handling" (:uri request))
              (.printStackTrace e)))
        {:status 400
         :body   (->body e message expected?)}))))

(defn wrap-exceptions [delegate-handler]
  (partial handle delegate-handler))
