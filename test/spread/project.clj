(defproject house.jux/test.spread "2024.09.28"

  :description "Support for highly expressive, two-dimensional tests represented as spreadsheets."
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/test/spread"

  :license {:name "BSD 3-Clause"
            :url "https://github.com/klauswuestefeld/simple-clj/blob/master/LICENSE"}

  :dependencies [[org.clojure/data.csv "1.0.0"]
                 [ring/ring-core "1.6.2"]
                 [ring/ring-jetty-adapter "1.6.2"]
                 [house.jux/biz.command-result "2024.09.05"]
                 [house.jux/exceptions "2024.06.10"]
                 [house.jux/http.api "2024.06.11"]
                 [house.jux/http.exceptions "2024.06.24"]
                 [house.jux/http.pprint "2024.06.10"]
                 [house.jux/http.json-codec "2024.06.20"]
                 [simple/check "2024.06.10"]]

  :plugins [[lein-parent "0.3.9"]]
  :parent-project {:coords  [house.jux/parent-project "2024.06.10"]
                   :inherit [:deploy-repositories :dependencies]}

  :test-paths ["test"])
