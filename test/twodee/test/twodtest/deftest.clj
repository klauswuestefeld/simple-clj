(ns twodtest.deftest
  (:require [clojure.test :refer [deftest]]
            [house.jux--.test.twodee-- :as subject]))

(deftest twodee-tests
  (subject/run-all-tests!))
