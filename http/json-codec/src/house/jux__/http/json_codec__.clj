(ns house.jux--.http.json-codec--
  (:require
   [cheshire.core :as json])
  (:import
   [java.io BufferedReader BufferedWriter InputStream InputStreamReader OutputStreamWriter PipedInputStream PipedOutputStream]))

(defn- pipe-json-if-necessary [value]
  (if (nil? value)
    "null"
    (let [input  (PipedInputStream. (* 1024 32)) ; 32k buffer
          output (-> input PipedOutputStream. OutputStreamWriter. BufferedWriter.)]
      (future
        (try
          (json/generate-stream value output)
          (finally (.close output))))
      input)))

(defn- decode [input]
  (-> input InputStreamReader. BufferedReader. (json/parse-stream keyword)))

(defn- decode-if-necessary [body]
  (cond-> body
    (instance? InputStream body) decode))

(defn wrap [delegate]
  (fn [request]
    (let [request (update request :body decode-if-necessary)
          response (delegate request)]
      (update response :body pipe-json-if-necessary))))
