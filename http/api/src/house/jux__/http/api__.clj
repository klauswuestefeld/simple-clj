(ns house.jux--.http.api--
  (:require [clojure.string :refer [split]]))

(defn- all-params [request]
  (merge
   (-> request :params)
   (-> request :body)))

(defn- call-api [api-fn user request]
  (let [endpoint (-> request :uri (split #"/") last)
        params   (-> request all-params)]
    #_(pprint ["Params:" params]) ; This is extremely slow and causes timeout for large payloads such as upsert-architecture
    (api-fn endpoint user params)))

(defn- handle [api-fn request {:keys [anonymous?]}]
  (let [user (-> request :session :user)]
    (if (or user anonymous?)
      {:status 200
       :body   (->> request (call-api api-fn user))}
      {:status 401
       :body   "Unauthorized API Call"})))

(defn wrap-api [delegate uri-prefix api-fn & [options]]
  (fn [request]
    (if (-> request :uri (.startsWith uri-prefix))
      (handle api-fn request options)
      (delegate request))))
