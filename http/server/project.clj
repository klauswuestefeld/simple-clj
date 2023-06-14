(defproject house.jux/http.server "2023.06.13"

  :description "Server with some request handling functionalities."
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/http/server"

  :license {:name "BSD 3-Clause"
            :url "https://github.com/klauswuestefeld/simple-clj/blob/master/LICENSE"}

  :pom-addition [:properties
                 ["maven.compiler.source" "17"]
                 ["maven.compiler.target" "17"]]

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [jumblerg/ring-cors "3.0.0"]
                 [ring/ring-jetty-adapter "1.10.0"]
                 [cheshire "5.11.0"]
                 [house.jux/exceptions "2023.06.13"]]

  :repositories [["clojars" {:sign-releases false}]])
