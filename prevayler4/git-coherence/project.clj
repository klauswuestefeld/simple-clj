(defproject house.jux/prevayler4.git-coherence "2025-02-20"
  :description "Provides a prevayler business function wrapper that is code version coherent"
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/prevayler4/git-coherence"
  :license {:name "BSD 3-Clause"
            :url "https://github.com/klauswuestefeld/simple-clj/blob/master/LICENSE"}


  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;;
  ;; Keep this dependencies list in sync with deps.edn !!!
  ;;
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  :dependencies [[prevayler-clj/prevayler4 "2024.03.18"]
                 [org.clojure/tools.namespace "1.4.4"]
                 [simple/check "2024.06.09"]]

  :plugins [[lein-parent "0.3.9"]]
  :parent-project {:coords  [house.jux/parent-project "2024.06.10"]
                   :inherit [:deploy-repositories :dependencies]}
            
  :profiles {:dev {:dependencies [[babashka/fs "0.5.24"]
                                  [babashka/process "0.5.22"]
                                  [http-kit/http-kit "2.5.0"]
                                  [prestancedesign/get-port "0.1.1"]
                                  [diehard/diehard "0.11.12"]]}})
