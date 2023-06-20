(ns house.jux--.http.server--
  (:require
   [house.jux--.http.server--.cors :refer [wrap-cors]]
   [house.jux--.http.server--.exceptions :refer [wrap-exceptions]]
   [house.jux--.http.server--.github-webhook :refer [wrap-redeploy!]]
   [house.jux--.http.server--.pprint :refer [wrap-pprint]]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]])
  (:import [java.util TimeZone]))

(def server (atom nil))

#_(defn- wrap-keyword-params-if-necessary [delegate keyword-params?]
  (cond-> delegate
    keyword-params? wrap-keyword-params))

#_(defn- start-http-server! [{:keys [handler service-name cors-prefixes port keyword-params?]}]
  (-> handler
      (wrap-keyword-params-if-necessary keyword-params?)
      (wrap-cors cors-prefixes)
      (wrap-redeploy! service-name #(.stop @server))
      (wrap-exceptions)
      (wrap-pprint)
      (wrap-params)
      (run-jetty {:host "127.0.0.1" ; Accept only local requests, such as reverse proxy (Ex: Caddy)
                  :port (if port (Integer. ^String port) 8080)
                  :join? false})))

(defn- start-http-server! [handler]
  (wrap-pprint handler))

(defn start! [options]
  #_(-> "UTC" TimeZone/getTimeZone TimeZone/setDefault)
  #_(System/setProperty "file.encoding" "UTF-8")
    (start-http-server! options)
  #_(reset! server (start-http-server! options)))
