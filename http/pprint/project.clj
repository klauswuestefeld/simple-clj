(defproject house.jux/http.pprint "2024.06.09"

  :description "Wrapper that prints summaries of incoming requests"
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/http/pprint"

  :plugins [[lein-parent "0.3.9"]]
  :parent-project {:coords  [house.jux/parent-project "2024.06.09"]
                   :inherit [:deploy-repositories :dependencies :license]})
