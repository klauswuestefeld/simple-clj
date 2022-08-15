(ns house.jux--.test.twodee.server
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [house.jux--.http.api-- :as api]
            [house.jux--.http.exceptions-- :refer [wrap-exceptions]]
            [house.jux--.http.pprint-- :refer [wrap-pprint]]
            [clojure.java.io :as java.io]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.response :refer [resource-response]]
            [house.jux--.test.twodee.csv :as csv]
            [house.jux--.test.twodee-- :as twodee]))

(def not-found (constantly {:status 404, :body "Not found"}))
(def port 7357)

(defn wrap-index-html [delegate]
  (fn [request]
    (if (-> request :uri (= "/"))
      (resource-response "house/jux__/test/twodee/index.html")
      (delegate request))))

(defn- save-and-run [_endpoint _user {:keys [filename spreadsheet-data]}]
  (when filename
    (csv/write! twodee/all-spreadsheets-folder filename spreadsheet-data))
  (twodee/run-tests!))

(defn- file-tree [file]
  (cond-> {:name (.getName file)}
    (.isDirectory file) (assoc :children (map file-tree (.listFiles file)))))

(defn- test-tree [_endpoint _user _]
  (file-tree (clojure.java.io/file twodee/all-spreadsheets-folder)))

(defn- csv-read [_endpoint _user {:keys [filename]}]
  (csv/read! twodee/all-spreadsheets-folder filename nil nil nil))

(defonce server (atom nil))
(defn restart! []
  (some-> @server (.stop))
  (reset! server
          (-> not-found
              (wrap-index-html)
              (api/wrap-api "/api/get-test-tree" test-tree {:anonymous? true})
              (api/wrap-api "/api/csv-read"  csv-read {:anonymous? true})
              (api/wrap-api "/api/save-and-run" save-and-run {:anonymous? true})
              (wrap-exceptions)
              (wrap-pprint)
              (wrap-keyword-params)
              (wrap-params)
              (run-jetty {:port  port
                          :join? false})))
  (println "Twodee restarted. Listening on port" port))

(restart!)