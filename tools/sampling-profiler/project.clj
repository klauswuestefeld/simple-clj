(defproject house.jux/tools.sampling-profiler "2024.11.08"

  :description "A lightweight profiler that samples Thread/allStackTraces and allows you to filter relevant stack frames."
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/tools/sampling-profiler"

  :license {:name "BSD 3-Clause"
            :url "https://github.com/klauswuestefeld/simple-clj/blob/master/LICENSE"}

  :dependencies []

  :plugins [[lein-parent "0.3.9"]]
  :parent-project {:coords  [house.jux/parent-project "2024.06.10"]
                   :inherit [:deploy-repositories :dependencies]})
