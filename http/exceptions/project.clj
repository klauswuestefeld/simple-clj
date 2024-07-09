(defproject house.jux/http.exceptions "2024.07.09"

  :description "Wrapper that handles API thrown exceptions with a 400 status code."
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/http/exceptions"

  :license {:name "BSD 3-Clause"
            :url "https://github.com/klauswuestefeld/simple-clj/blob/master/LICENSE"}

  :plugins [[lein-parent "0.3.9"]]
  :parent-project {:coords  [house.jux/parent-project "2024.06.10"]
                   :inherit [:deploy-repositories :dependencies]}

  :dependencies [[house.jux/exceptions "2024.06.10"]])
