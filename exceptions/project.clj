(defproject house.jux/exceptions "2024.06.07"

  :description "Exception utilities"
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/exceptions"

  :license {:name "BSD 3-Clause"
            :url "https://github.com/klauswuestefeld/simple-clj/blob/master/LICENSE"}
  
  :dependencies [[org.clojure/clojure "1.11.1"]]
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/CLOJARS_USERNAME
                                    :password :env/CLOJARS_PASSWORD
                                    :sign-releases false}]]
  :pom-addition [:properties
                 ["maven.compiler.source" "17"]
                 ["maven.compiler.target" "17"]])
