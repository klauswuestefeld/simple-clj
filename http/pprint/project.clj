(defproject house.jux/http.pprint "2022.10.03"

  :description "Wrapper that prints summaries of incoming requests"
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/http/pprint"

  :license {:name "BSD 3-Clause"
            :url "https://github.com/klauswuestefeld/simple-clj/blob/master/LICENSE"}

  :pom-addition [:properties
                 ["maven.compiler.source" "11"]
                 ["maven.compiler.target" "11"]]
                 
  :dependencies [[org.clojure/clojure "1.11.1"]]

  :repositories [["clojars" {:sign-releases false}]])
