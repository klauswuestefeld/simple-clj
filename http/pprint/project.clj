(defproject house.jux/http.pprint "2024.06.10"

  :description "Wrapper that prints summaries of incoming requests."
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/http/pprint"

  :license {:name "BSD 3-Clause"
            :url "https://github.com/klauswuestefeld/simple-clj/blob/master/LICENSE"}

  :plugins [[lein-parent "0.3.9"]]
  :parent-project {:coords  [house.jux/parent-project "2024.06.10"]
                   :inherit [:deploy-repositories :dependencies]})
