(defproject house.jux/http.json-codec "2024.07.15"

  :description "Ring-style wrapper to transparently encode and decode JSON to/from Clojure values."
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/http/json-codec"

  :license {:name "BSD 3-Clause"
            :url "https://github.com/klauswuestefeld/simple-clj/blob/master/LICENSE"}

  :plugins [[lein-parent "0.3.9"]]
  :parent-project {:coords  [house.jux/parent-project "2024.06.10"]
                   :inherit [:deploy-repositories :dependencies]})
