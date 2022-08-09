(defproject house.jux/http.exceptions "2022.08.03-SNAPSHOT"

  :description "Wrapper that handles API thrown exceptions with a 400 status code"
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/http/exceptions"

  :license {:name "BSD 3-Clause"
            :url "https://github.com/klauswuestefeld/simple-clj/blob/master/LICENSE"}

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [cheshire "5.8.0"]]

  :repositories [["clojars" {:sign-releases false}]])