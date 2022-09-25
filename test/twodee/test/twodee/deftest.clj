(ns twodee.deftest
  (:require [clojure.test :refer [deftest is]]
            [house.jux--.test.twodee-- :as subject]))

(deftest twodee-tests
  (is (= (subject/run-all-tests!)
         :OK)))
