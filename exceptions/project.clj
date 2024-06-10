(defproject house.jux/exceptions "2024.06.09"
  :description "Exception utilities"
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/exceptions"

  :plugins [[lein-parent "0.3.9"]]
  :parent-project {:coords  [house.jux/parent-project "2024.06.09"]
                   :inherit [:deploy-repositories :dependencies :license]})
