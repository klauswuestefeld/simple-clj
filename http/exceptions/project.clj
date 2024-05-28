(defproject house.jux/http.exceptions "2024.05.28-SNAPSHOT"

  :description "Wrapper that handles API thrown exceptions with a 400 status code"
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/http/exceptions"

  :license {:name "BSD 3-Clause"
            :url "https://github.com/klauswuestefeld/simple-clj/blob/master/LICENSE"}

  :pom-addition [:properties
                 ["maven.compiler.source" "11"]
                 ["maven.compiler.target" "11"]]

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [cheshire "5.8.0"]
                 [house.jux/exceptions "2024.05.28-SNAPSHOT"]])
