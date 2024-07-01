(defproject house.jux/biz.command-result "2024.07.01"

  :description "Support for commands to return a result without polluting the state (uses a dynamic var)."
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/biz/command-result"

  :license {:name "BSD 3-Clause"
            :url "https://github.com/klauswuestefeld/simple-clj/blob/master/LICENSE"}

  :plugins [[lein-parent "0.3.9"]]
  :parent-project {:coords  [house.jux/parent-project "2024.06.10"]
                   :inherit [:deploy-repositories :dependencies]})
