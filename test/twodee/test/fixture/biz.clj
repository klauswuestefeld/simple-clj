(ns fixture.biz
  (:require [house.jux--.biz.user-- :refer [user]]))

(defn create-new-profile [state profile]
  (let [state (->> (assoc profile :created-by (user))
                   (assoc state   :profile))]
    (assoc state :result "Profile Created")))

(defn created-profile [state]
  (:profile state))

(defn update-profile [state profile]
  (when (-> profile :name (= "BOOM"))
    (throw (RuntimeException. "Invalid name")))
  (let [state (-> state
                  (update :profile merge profile)
                  (assoc-in [:profile :last-updated-by] (user)))]
    (assoc state :result "Profile Updated")))

(defn registered-profiles [state]
  (if (:profile state) 1 0))

(defn search-by-name [state name-arg]
  (if (-> state :profile :name (= name-arg))
    (:profile state)
    nil))