(defproject house.jux/parent-project "2024.06.10"
  :description "Parent project for shared configuration"

  :dependencies [[org.clojure/clojure "1.11.3"]]

  :license {:name "Proprietary"
            :url "https://en.wikipedia.org/wiki/Proprietary_software"}

  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/CLOJARS_USERNAME
                                    :password :env/CLOJARS_PASSWORD
                                    :sign-releases false}]])