(ns house.jux--.test.twodee--.csv
  (:require [clojure.string]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]))

(defn read!
  [relative-path]
  (with-open [reader (io/reader relative-path)]
    (doall
     (csv/read-csv reader))))

(defn write!
  [relative-path lines]
  (with-open [writer (io/writer relative-path)]
    (csv/write-csv writer lines)))
