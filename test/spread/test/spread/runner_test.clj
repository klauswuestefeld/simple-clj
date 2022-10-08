(ns spread.runner-test
  (:require [clojure.test :refer [deftest is]]
            [house.jux--.test.spread-- :as subject]))

(deftest spread-tests
  (is (= (subject/run-all-tests!)
         :OK)))
