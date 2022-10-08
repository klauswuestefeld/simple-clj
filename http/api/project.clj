(defproject house.jux/http.api "2022.10.03"

  :description "Wrapper that handles API calls and redirects it to corresponding functions"
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/http/api"

  :license {:name "BSD 3-Clause"
            :url "https://github.com/klauswuestefeld/simple-clj/blob/master/LICENSE"}

  :pom-addition [:properties
                 ["maven.compiler.source" "11"]
                 ["maven.compiler.target" "11"]]

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [cheshire "5.8.0"]]

  :repositories [["clojars" {:sign-releases false}]])
