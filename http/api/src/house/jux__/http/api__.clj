(ns house.jux--.http.api--
  (:require [cheshire.core :as json]
            [clojure.pprint :refer [pprint]]
            [clojure.string :refer [split]])
  (:import (java.io InputStream)))

(defn- slurp-body [body]
  (cond-> body (instance? InputStream body) slurp))

(defn- all-params [request]
  (merge
   (-> request :params)
   (-> request :body slurp-body (json/parse-string keyword))))

(defn- call-api [api-fn user request]
  (let [endpoint (-> request :uri (split #"/") last)
        params   (-> request all-params)]
    (pprint ["Params:" params])
    (api-fn endpoint user params)))

(defn- handle [api-fn request {:keys [anonymous?]}]
  (let [user (-> request :session :user)]
    (if (or user anonymous?)
      {:status 200
       :body   (->> request (call-api api-fn user) json/generate-string)}
      {:status 401
       :body   "Unauthorized"})))

(defn wrap-api [delegate uri-prefix api-fn & [options]]
  (fn [request]
    (if (-> request :uri (.startsWith uri-prefix))
      (handle api-fn request options)
      (delegate request))))
