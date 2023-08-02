(defproject house.jux/biz.user "2022.10.03"

  :description "Support for an implicit *user* argument (a dynamic var)."
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/biz/user"

  :license {:name "BSD 3-Clause"
            :url "https://github.com/klauswuestefeld/simple-clj/blob/master/LICENSE"}

  :pom-addition [:properties
                 ["maven.compiler.source" "11"]
                 ["maven.compiler.target" "11"]]

  :dependencies [[org.clojure/clojure "1.11.1"]])
