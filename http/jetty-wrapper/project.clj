(defproject house.jux/http.jetty-wrapper "2023.06.19"

  :description "Wraps jetty server"
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/http/server"

  :license {:name "BSD 3-Clause"
            :url "https://github.com/klauswuestefeld/simple-clj/blob/master/LICENSE"}

  :pom-addition [:properties
                 ["maven.compiler.source" "17"]
                 ["maven.compiler.target" "17"]]

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [ring/ring-jetty-adapter "1.10.0"]]

  :repositories [["clojars" {:sign-releases false}]])
