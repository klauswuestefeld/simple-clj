(ns house.jux--.http.wrapper--.cors
  (:require [clojure.string :refer [join]]))

(def default-headers
  #{"Cache-Control"
    "Content-Language"
    "Content-Type"
    "Expires"
    "Last-Modified"
    "Pragma"})

(defn- clean [map] (into {} (remove (comp nil? val) map)))

(defn- with-cors-headers [origin response]
  (update response :headers (comp clean assoc)
          "Access-Control-Allow-Origin"      origin
          "Access-Control-Allow-Credentials" "true"))

(def one-day "86400")

(defn- ->preflight [{:strs [access-control-request-headers access-control-request-method]}]
  {"Access-Control-Max-Age"       one-day
   "Access-Control-Allow-Headers" access-control-request-headers
   "Access-Control-Allow-Methods" access-control-request-method})

(defn- expose-headers [response]
  (if-let [headers (:headers response)]
    (update response :headers assoc 
            "Access-Control-Expose-Headers" 
            (->> headers keys (remove default-headers) (join ", ")))
    response))

(defn- allow-origins [req delegate allowed-prefixes]
  (let [req-hdrs (req :headers)
        origin   (req-hdrs "origin")
        allowed? (and origin
                      (some #(.startsWith origin %) allowed-prefixes))]
    (if allowed?
      (with-cors-headers origin
        (if (contains? req-hdrs "access-control-request-method")
          {:status 204
           :headers (->preflight req-hdrs)}
          (expose-headers (delegate req))))
      (do
        (println "CORS not allowed. Origin:" origin)
        {:status 403}))))

(defn wrap-cors [delegate allowed-prefixes]
  (fn [req] (allow-origins req delegate allowed-prefixes)))
