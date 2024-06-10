(defproject house.jux/parent-project "2024.06.08"
  :description "Parent project for shared configuration"

  :dependencies [[org.clojure/clojure "1.11.3"]]

  :license {:name "BSD 3-Clause"}
            :url "https://github.com/klauswuestefeld/simple-clj/blob/master/LICENSE"

  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/CLOJARS_USERNAME
                                    :password :env/CLOJARS_PASSWORD
                                    :sign-releases false}]])