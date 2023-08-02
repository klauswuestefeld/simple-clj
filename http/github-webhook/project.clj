(defproject house.jux/http.github-webhook "0.0.2"

  :description "Github webhook for redeploy."
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/http/github-webhook"

  :license {:name "BSD 3-Clause"
            :url "https://github.com/klauswuestefeld/simple-clj/blob/master/LICENSE"}

  :pom-addition [:properties
                 ["maven.compiler.source" "17"]
                 ["maven.compiler.target" "17"]]

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [cheshire "5.11.0"]])
