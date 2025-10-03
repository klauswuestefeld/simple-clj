(ns house.jux--.http.exceptions--
  (:require
   [clojure.stacktrace :as stacktrace]
   [house.jux--.exceptions-- :refer [message expected?]]))

(defn- ->body [e message expected?]
  (merge (if expected?
           {:error message, :expected true}
           {:error message, :stacktrace (with-out-str (stacktrace/print-cause-trace e))}) (ex-data e)))

(defn- drain! [^java.io.InputStream in] ; Otherwise uploads from the browser proxied through Caddy (HTTP2) will stay "pending" forever.
  (let [buf (byte-array 8192)]
      (loop []
        (let [n (.read in buf)]
          (when (pos? n) (recur))))))
  
(defn handle [delegate-handler request]
  (try

    (delegate-handler request)

    (catch Exception e
      (drain! (:body request))
      (let [message   (message e)
            expected? (expected? e)]
        (if expected?
          (println message (:uri request))
          (do (println "Exception handling" (:uri request))
              (.printStackTrace e)))
        {:status (or (:status (ex-data e)) 400)
         :body   (->body e message expected?)}))))

(defn wrap-exceptions [delegate-handler]
  (fn [request]
    (handle delegate-handler request)))
