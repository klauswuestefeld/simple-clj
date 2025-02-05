(defproject house.jux/prevayler4.coherence "0.0.1-SNAPSHOT"
  :description "Provides a prevayler business function wrapper that is code version coherent"
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/prevayler4/coherence"
  :license {:name "BSD 3-Clause"
            :url "https://github.com/klauswuestefeld/simple-clj/blob/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [prevayler-clj/prevayler4 "2020.11.14"]
                 [org.clojure/tools.namespace "1.4.4"]
                 [simple/check "2024.06.09"]]
  :profiles {:dev {:dependencies [[babashka/fs "0.5.24"]
                                  [http-kit/http-kit "2.5.0"]
                                  [prestancedesign/get-port "0.1.1"]
                                  [babashka/babashka "1.12.196"]]}})
