(ns house.jux--.test.twodee.layout
  (:require [clojure.java.io :as java.io]))

(def file-suffix
  ".layout.edn")

(defn- layout-file? [filename]
  (-> filename java.io/file .exists))

(defn layout-get [relative-path]
  (let [filename (str relative-path file-suffix)]
    (when (layout-file? filename)
      (-> filename slurp read-string))))

(defn layout-save [relative-path dimensions]
  (let [filename (str relative-path file-suffix)]
    (spit filename dimensions)))
