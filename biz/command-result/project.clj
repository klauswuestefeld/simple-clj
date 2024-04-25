(defproject house.jux/biz.command-result "2024.04.25"

  :description "Support for *result-atom* (a dynamic var)."
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/biz/command-result"

  :license {:name "BSD 3-Clause"
            :url "https://github.com/klauswuestefeld/simple-clj/blob/master/LICENSE"}

  :pom-addition [:properties
                 ["maven.compiler.source" "11"]
                 ["maven.compiler.target" "11"]]

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [simple/check "2.0.0"]])
