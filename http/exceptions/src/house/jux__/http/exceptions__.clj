(ns house.jux--.http.exceptions--
  (:require
    [cheshire.core :as json]
    clojure.stacktrace
   ))

(defn handle [delegate-handler request]
  (try

    (delegate-handler request)

    (catch Exception e
      (clojure.stacktrace/print-cause-trace e)
      {:status 400
       :body   (json/generate-string {:message (or (.getMessage e) (-> e .getClass str))
                                      :ex-data (ex-data e)})})))

(defn wrap-exceptions [delegate-handler]
  (partial handle delegate-handler))