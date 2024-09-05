(defproject house.jux/biz.timestamp "2024.09.05"

  :description "Support for *timestamp* (a dynamic var)."
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/biz/timestamp"

  :license {:name "BSD 3-Clause"
            :url "https://github.com/klauswuestefeld/simple-clj/blob/master/LICENSE"}

  :plugins [[lein-parent "0.3.9"]]
  :parent-project {:coords  [house.jux/parent-project "2024.06.10"]
                   :inherit [:deploy-repositories :dependencies]})
