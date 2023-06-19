(ns house.jux--.http.jetty-wrapper--
  (:require
   [ring.adapter.jetty :refer [run-jetty]])
  (:import [java.util TimeZone]))

(def server (atom nil))

(defn- start-http-server! [{:keys [handler port]}]
  (-> handler
      (run-jetty {:host "127.0.0.1" ; Accept only local requests, such as reverse proxy (Ex: Caddy)
                  :port (if port (Integer. ^String port) 8080)
                  :join? false})))

(defn start! [options]
  (-> "UTC" TimeZone/getTimeZone TimeZone/setDefault)
  (System/setProperty "file.encoding" "UTF-8")

  (reset! server (start-http-server! options)))
