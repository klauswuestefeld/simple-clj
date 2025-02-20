(ns house.jux--.http.json-codec--
  (:require
   [cheshire.core :as json])
  (:import
   [java.io BufferedReader BufferedWriter InputStream InputStreamReader OutputStreamWriter PipedInputStream PipedOutputStream]))


(defn- input-stream? [value]
  (instance? InputStream value))

(defn- pipe-json [value]
  (let [input  (PipedInputStream. (* 1024 32)) ; 32k buffer
        output (-> input PipedOutputStream. OutputStreamWriter. BufferedWriter.)]
    (future
      (try
        (json/generate-stream value output)
        (finally (.close output))))
    input))

(defn- pipe-json-if-necessary [value]
  (cond
    (nil? value) "null"
    (input-stream? value) value
    :else (pipe-json value)))

(defn- decode [input]
  (-> input InputStreamReader. BufferedReader. (json/parse-stream keyword)))

(defn- decode-if-necessary [body]
  (cond-> body
    (input-stream? body) decode))

(defn decode-request [request]
  (update request :body decode-if-necessary))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn wrap-decode-request [delegate]
  (fn [request]
    (delegate (decode-request request))))

(defn- encode-response [response]
  (if (:handled response)
    response
    (-> response
        (assoc-in [:headers "Content-Type"] "application/json")
        (update :body pipe-json-if-necessary))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn wrap-encode-response [delegate]
  (fn [request]
    (encode-response (delegate request))))

(defn wrap [delegate]
  (fn [request]
    (-> request
        decode-request
        delegate
        encode-response)))
