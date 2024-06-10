(defproject simple/check "2024.06.09"

  :description "An assertion that cannot be turned off."
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/check"

  :plugins [[lein-parent "0.3.9"]]
  :parent-project {:coords  [house.jux/parent-project "2024.06.09"]
                   :inherit [:deploy-repositories :dependencies :license]}

  :dependencies [[house.jux/exceptions "2024.06.09"]])
