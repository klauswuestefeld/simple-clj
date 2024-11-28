(defproject house.jux/http.api "2024.11.28"

  :description "Wrapper that handles API calls and redirects it to corresponding functions"
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/http/api"

  :license {:name "BSD 3-Clause"
            :url "https://github.com/klauswuestefeld/simple-clj/blob/master/LICENSE"}

  :plugins [[lein-parent "0.3.9"]]
  :parent-project {:coords  [house.jux/parent-project "2024.06.10"]
                   :inherit [:deploy-repositories :dependencies]})
