(ns house.jux--.test.spread--.layout
  (:require [clojure.java.io :as java.io]
            [clojure.string :as string]))

(def file-suffix ".layout.edn")

(defn- layout-file? [filename]
  (-> filename java.io/file .exists))

(defn- path->filename [path]
  (string/replace path #".csv" file-suffix))

(defn layout-get [relative-path]
  (let [filename (path->filename relative-path)]
    (when (layout-file? filename)
      (-> filename slurp read-string))))

(defn layout-save [relative-path dimensions]
  (spit (path->filename relative-path) dimensions))
