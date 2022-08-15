(ns twodee.test.twodee-test
  (:require [clojure.test :refer [deftest is]]
            [house.jux--.test.twodee-- :as subject]))

(def parsed-csv
  (subject/parse-csv "test/twodtest/test-namespace.biz/create-new-profile.csv"))

(deftest building-test-map
  (is (= {:title "An user can create a new profile",
          :initial-state "{}",
          :queries
          [["tester" "created-profile" ":name"]
           ["tester" "created-profile" ":phone"]
           ["tester" "created-profile" ":age"]
           ["tester" "created-profile" ":created-by"]
           ["tester" "created-profile" ":last-updated-by"]],
          :initial-results ["nil" "nil" "nil" "nil" "nil"],
          :steps
          [{:command
            {:user "tester",
             :function "create-new-profile",
             :params "{:name  \"New User\"  :phone 444 :age 40 }",
             :result "*",
             :result-coords {:line 6, :column "D"}},
            :query-results ["\"New User\"" "444" "40" "tester" "nil"]}
           {:command
            {:user "tester",
             :function "update-profile",
             :params "{:phone 555}",
             :result "*",
             :result-coords {:line 7, :column "D"}},
            :query-results ["\"New User\"" "555" "40" "tester" "tester"]}]}
         (subject/csv->test-map parsed-csv))))