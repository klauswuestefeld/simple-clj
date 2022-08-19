(ns house.jux--.test.twodee.server
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [house.jux--.http.api-- :as api]
            [house.jux--.http.exceptions-- :refer [wrap-exceptions]]
            [house.jux--.http.pprint-- :refer [wrap-pprint]]
            [clojure.java.io :as java.io]
            [clojure.string :as string]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.response :refer [resource-response]]
            [house.jux--.test.twodee.csv :as csv]
            [house.jux--.test.twodee.layout :as layout]
            [house.jux--.test.twodee-- :as twodee]))

(def not-found (constantly {:status 404, :body "Not found"}))
(def port 7357)

(defn wrap-index-html [delegate]
  (fn [request]
    (if (-> request :uri (= "/"))
      (resource-response "house/jux__/test/twodee/index.html")
      (delegate request))))

(defn- run-tests [_endpoint _user _]
  (twodee/run-all-tests!))

(defn- save-and-run [endpoint user {:keys [filename spreadsheet-data spreadsheet-dimensions] :as params}]
  (let [relative-path   (str twodee/all-spreadsheets-folder filename)]
    (when filename
      (csv/write! relative-path spreadsheet-data))
    (layout/layout-save relative-path spreadsheet-dimensions)
    (run-tests endpoint user params)))

(defn- csv-read [_endpoint _user {:keys [filename]}]
  (let [relative-path   (str twodee/all-spreadsheets-folder filename)
        data            (csv/read! relative-path)
        dimensions      (layout/layout-get relative-path)]
    (cond-> {:data data}
     dimensions (assoc :dimensions dimensions))))

(defn- get-test-files [file]
  (remove (fn [file]
            (string/ends-with? (.getName file) layout/file-suffix)) (.listFiles file)))

(defn- file-tree [file]
  (cond-> {:name (.getName file)}
    (.isDirectory file) (assoc :children (map file-tree (get-test-files file)))))

(defn- test-tree [_endpoint _user _]
  (file-tree (java.io/file twodee/all-spreadsheets-folder)))

(defonce server (atom nil))
(defn restart! []
  (some-> @server (.stop))
  (reset! server
          (-> not-found
              (wrap-index-html)
              (api/wrap-api "/api/run" run-tests {:anonymous? true})
              (api/wrap-api "/api/save-and-run" save-and-run {:anonymous? true})
              (api/wrap-api "/api/csv-read"  csv-read {:anonymous? true})
              (api/wrap-api "/api/get-test-tree" test-tree {:anonymous? true})
              (wrap-exceptions)
              (wrap-pprint)
              (wrap-keyword-params)
              (wrap-params)
              (run-jetty {:port  port
                          :join? false})))
  (println "Twodee restarted. Listening on port" port))

(restart!)