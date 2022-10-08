(ns unit.spread-test
  (:require [clojure.test :refer [deftest is]]
            [house.jux--.test.spread-- :as subject]))

(def parsed-csv
  (subject/parse-csv "test/spread/fixture.biz/create-new-profile.csv"))
;; TODO: fix the following test
(deftest building-test-map
  (is (= {:title "A user can create a new profile",
          :queries [["tester" "created-profile" ":name"]
                    ["tester" "created-profile" ":phone"]
                    ["tester" "created-profile" ":age"]
                    ["tester" "created-profile" ":created-by"]
                    ["tester" "created-profile" ":last-updated-by"]
                    ["tester" "search-by-name \"Bob\"" ":last-updated-by"]],
          :steps
          [{:command
            {:user "nil",
             :function "(fn [state & _ignored] state)",
             :params "",
             :result "*",
             :result-coords {:line 5, :column "D"}},
            :query-results ["nil" "nil" "nil" "nil" "nil" "nil"]}
           {:command
            {:user "tester",
             :function "create-new-profile",
             :params "{:name \"Bob\" :phone 444 :age 40}",
             :result "*",
             :result-coords {:line 6, :column "D"}},
            :query-results ["\"Bob\"" "444" "40" "tester" "nil" "nil"]}
           {:command
            {:user "tester",
             :function "update-profile",
             :params "{:phone 555}",
             :result "*",
             :result-coords {:line 7, :column "D"}},
            :query-results ["\"Bob\"" "555" "40" "tester" "tester" "tester"]}
           {:command
            {:user "tester",
             :function "update-profile",
             :params "{:name \"BOOM\"}",
             :result "Invalid name",
             :result-coords {:line 8, :column "D"}},
            :query-results
            ["\"Bob\"" "555" "40" "tester" "tester" "tester"]}]}
         (subject/csv->test-map parsed-csv))))
