(defproject house.jux/test.spread "2023.08.02"

  :description "Support for highly expressive, two-dimensional tests represented as spreadsheets."
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/test/spread"

  :license {:name "BSD 3-Clause"
            :url "https://github.com/klauswuestefeld/simple-clj/blob/master/LICENSE"}

  :pom-addition [:properties
                 ["maven.compiler.source" "11"]
                 ["maven.compiler.target" "11"]]

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/data.csv "1.0.0"]
                 [ring/ring-core "1.6.2"]
                 [ring/ring-jetty-adapter "1.6.2"]
                 [house.jux/biz.user "2022.10.03"]
                 [house.jux/http.api "2022.10.03"]
                 [house.jux/http.exceptions "2023.01.27"]
                 [house.jux/http.pprint "2022.10.03"]
                 [simple/check "2.0.0"]]

  :test-paths ["test"])
