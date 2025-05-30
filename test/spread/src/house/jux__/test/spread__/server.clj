(ns house.jux--.test.spread--.server
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [house.jux--.exceptions-- :as exceptions]
            [house.jux--.http.api-- :as api]
            [house.jux--.http.exceptions-- :refer [wrap-exceptions]]
            [house.jux--.http.pprint-- :refer [wrap-pprint]]
            [house.jux--.http.json-codec-- :as json-codec]
            [clojure.java.io :as java.io]
            [clojure.string :as string]
            [ring.middleware.content-type :refer [content-type-response]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.resource :refer [resource-request]]
            [ring.util.response :refer [resource-response]]
            [house.jux--.test.spread--.csv :as csv]
            [house.jux--.test.spread--.layout :as layout]
            [house.jux--.test.spread-- :as spread]))

(def not-found (constantly {:status 404, :body "Not found"}))
(def port 7357)

(defn wrap-frontend [delegate]
  (fn [request]
    (if (-> request :uri (= "/"))
      (resource-response "house/jux__/test/spread__/index.html")
      (if-let [resource-response (resource-request request "house/jux__/test/spread__")]
        (content-type-response resource-response request)
        (delegate request)))))

(defn- unpack-actual-exception-if-necessary [exception]
  (let [actual (or (-> exception ex-data :actual-exception)
                   exception)
        data (-> exception ex-data (dissoc :actual-exception) (or {}))
        msg (or (.getMessage actual) (str (.getClass actual)))]
    (if (:omit-stacktrace data)
      (exceptions/expected msg data)
      (ex-info msg data actual))))

(defn- run-tests [_endpoint _user _]
  (try
    (spread/run-all-tests!)
    (catch RuntimeException e
      (throw (unpack-actual-exception-if-necessary e)))))

(defn- ->relative-path [filename]
  (str spread/all-spreadsheets-folder filename))

(defn- update-layout
  ([relative-path spreadsheet-dimensions]
   (layout/layout-save relative-path spreadsheet-dimensions))
  ([_endpoint _user {:keys [filename spreadsheet-dimensions]}]
   (update-layout (->relative-path filename) spreadsheet-dimensions)))

(defn- save-and-run [endpoint user {:keys [filename spreadsheet-data spreadsheet-dimensions] :as params}]
  (let [relative-path (->relative-path filename)]
    (when filename
      (csv/write! relative-path spreadsheet-data)
      (update-layout relative-path spreadsheet-dimensions))
    (run-tests endpoint user params)))

(defn- accumulate-general-tests [acc file]
  (if (= file spread/all-spreadsheets-folder)
    acc
    (let [parent-file  (.getParentFile file)
          require-file (-> parent-file .getPath (str "/" spread/require-filename-suffix) java.io/file)
          acc (if (.exists require-file)
                (conj acc (csv/read! require-file))
                acc)]
      (accumulate-general-tests acc parent-file))))

(defn- require-list [relative-path]
  (let [specific-test (string/replace relative-path #".csv" ".require.csv")]
    (cond-> (accumulate-general-tests #{} (java.io/file relative-path))
      (-> specific-test
          java.io/file
          .exists)    (conj (csv/read! specific-test)))))

(defn- csv-read [_endpoint _user {:keys [filename]}]
  (let [relative-path   (->relative-path filename)
        data            (csv/read! relative-path)
        dimensions      (layout/layout-get relative-path)
        {:keys [commands initial-results queries]} (spread/cells-info data)
        requires        (require-list relative-path)]
    (cond-> {:data            data
             :commands        commands
             :initial-results initial-results
             :queries         queries
             :requires        requires}
      dimensions (assoc :dimensions dimensions))))

(defn- relevant? [file]
  (or (.isDirectory file)
      (spread/test-file? file)))

(def all-test-dirs
  (->> spread/all-spreadsheets-folder
       java.io/file
       file-seq
       (filter #(.isDirectory %))
       (map #(.getName %))
       set))

(defn- dir-and-file? [{:keys [name]}]
  (and (.endsWith name ".csv")
       (contains? all-test-dirs (string/replace name
                                                (re-pattern ".csv")
                                                ""))))

(defn- file-tree [file]
  (cond-> {:name (.getName file)
           :path (string/replace (.getPath file) (re-pattern "test/spread/") "")}
    (.isDirectory file) (assoc :children
                               (->> file
                                    .listFiles
                                    (filter relevant?)
                                    (map file-tree)
                                    (remove dir-and-file?)))))

(defn- test-tree [_endpoint _user _]
  (let [namespaces (->> spread/all-spreadsheets-folder
                        java.io/file
                        .listFiles
                        (filter #(.isDirectory %)))]
    (map file-tree namespaces)))

(def mutex (Object.))
(defn- wrap-single-request [delegate]
  (fn [request]
    (locking mutex
      (delegate request))))

(defonce server (atom nil))

(defn restart! []
  (some-> @server (.stop))
  (reset! server
          (-> not-found
              (api/wrap-api "/api/run" run-tests {:anonymous? true})
              (api/wrap-api "/api/save-and-run" save-and-run {:anonymous? true})
              (api/wrap-api "/api/update-layout" update-layout {:anonymous? true})
              (api/wrap-api "/api/csv-read"  csv-read {:anonymous? true})
              (api/wrap-api "/api/get-test-tree" test-tree {:anonymous? true})
              (wrap-single-request)
              (wrap-exceptions)
              (json-codec/wrap)
              (wrap-frontend)
              (wrap-pprint)
              (wrap-keyword-params)
              (wrap-params)
              (run-jetty {:port  port
                          :join? false})))
  (println (str "Spread restarted. Listening on http://localhost:" port)))

(comment
  (restart!))
