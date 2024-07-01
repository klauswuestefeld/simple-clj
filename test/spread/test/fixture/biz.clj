(ns fixture.biz
  (:require [house.jux--.biz.user-- :refer [user]]
            [house.jux--.biz.command-result-- :refer [set-result]]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn create-new-profile [state profile]
  (let [state (->> (assoc profile :created-by (user))
                   (assoc state   :profile))]
    (set-result "Profile Created")
    state))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn created-profile [state]
  (:profile state))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn update-profile [state profile]
  (when (-> profile :name (= "BOOM"))
    (throw (RuntimeException. "Invalid name")))
  (let [state (-> state
                  (update :profile merge profile)
                  (assoc-in [:profile :last-updated-by] (user)))]
    (set-result "Profile Updated")
    state))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn registered-profiles [state]
  (if (:profile state) 1 0))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn search-by-name [state name-arg]
  (if (-> state :profile :name (= name-arg))
    (:profile state)
    nil))