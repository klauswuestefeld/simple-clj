(defproject house.jux/prevayler4.transient "2022.07.27"

  :description "Transient implementation of prevayler-clj for tests"
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/prevayler4/transient"

  :license {:name "BSD 3-Clause"
            :url "https://github.com/klauswuestefeld/simple-clj/blob/master/LICENSE"}

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [prevayler-clj/prevayler4 "2020.11.14"]]
  
  :profiles {:dev {:dependencies [[midje "1.9.9"]]
                   :plugins [[lein-midje "3.1.3"]]}})
