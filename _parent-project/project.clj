(defproject parent-project "2024.06.07"
  :description "Parent project for shared configuration"
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/CLOJARS_USERNAME
                                    :password :env/CLOJARS_PASSWORD
                                    :sign-releases false}]])