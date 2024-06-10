(defproject house.jux/exceptions "2024.06.08"

  
  :description "Exception utilities"
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/exceptions"

  :plugins [[lein-parent "0.3.9"]] 
  :parent-project {:path "../_parent-project/project.clj"
                   :inherit [:deploy-repositories]}

  :license {:name "BSD 3-Clause"
            :url "https://github.com/klauswuestefeld/simple-clj/blob/master/LICENSE"}
  
  :dependencies [[org.clojure/clojure "1.11.1"]]

  :pom-addition [:properties
                 ["maven.compiler.source" "17"]
                 ["maven.compiler.target" "17"]])
