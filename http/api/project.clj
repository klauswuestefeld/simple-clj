(defproject house.jux/http.api "2024.06.10"

  :description "Wrapper that handles API calls and redirects it to corresponding functions"
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/http/api"

  :plugins [[lein-parent "0.3.9"]]
  :parent-project {:coords  [house.jux/parent-project "2024.06.09"]
                   :inherit [:deploy-repositories :dependencies :license]})
