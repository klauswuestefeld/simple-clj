(defproject house.jux/exceptions "2024.06.08"

  
  :description "Exception utilities"
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/exceptions"

  :parent [parent-project "2024.06.07" :relative-path "../_parent-project/pom.xml"]

  :license {:name "BSD 3-Clause"
            :url "https://github.com/klauswuestefeld/simple-clj/blob/master/LICENSE"}
  
  :dependencies [[org.clojure/clojure "1.11.1"]]

  :pom-addition [:properties
                 ["maven.compiler.source" "17"]
                 ["maven.compiler.target" "17"]])
