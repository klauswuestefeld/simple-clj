(defproject house.jux/biz.user "2024.06.10"

  :description "Support for an implicit *user* argument (a dynamic var)."
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/biz/user"

  :license {:name "BSD 3-Clause"
            :url "https://github.com/klauswuestefeld/simple-clj/blob/master/LICENSE"}

  :plugins [[lein-parent "0.3.9"]]
  :parent-project {:coords  [house.jux/parent-project "2024.06.10"]
                   :inherit [:deploy-repositories :dependencies]})
