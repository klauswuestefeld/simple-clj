(ns house.jux--.test.twodee.csv
  (:require [clojure.string]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]))

(defn read!
  [base-dir file-name _ _ _]
  (with-open [reader (io/reader (str base-dir file-name))]
    (doall
     (csv/read-csv reader))))

(defn write!
  [base-dir file-name lines]
  (with-open [writer (io/writer (str base-dir file-name))]
    (csv/write-csv writer lines)))
