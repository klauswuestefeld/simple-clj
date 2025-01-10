(defproject house.jux/exceptions "2025.01.10"
  :description "Exception utilities"
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/exceptions"

  :license {:name "BSD 3-Clause"
            :url "https://github.com/klauswuestefeld/simple-clj/blob/master/LICENSE"}

  :plugins [[lein-parent "0.3.9"]]
  :parent-project {:coords  [house.jux/parent-project "2024.06.10"]
                   :inherit [:deploy-repositories :dependencies]})
