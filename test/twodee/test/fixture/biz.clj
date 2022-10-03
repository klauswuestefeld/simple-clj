(ns fixture.biz
  (:require [house.jux--.biz.user-- :refer [user]]
            [simple.check2 :refer [check]]))

(defn create-new-profile [state _user profile]
  (check (= _user (user)) "user arg and implicit user must be the same, for now")
  (let [state (->> (assoc profile :created-by (user))
                   (assoc state   :profile))]
    (assoc state :result "Profile Created")))

(defn created-profile [state _user]
  (:profile state))

(defn update-profile [state _user profile]
  (let [state (-> state
                  (update :profile merge profile)
                  (assoc-in [:profile :last-updated-by] (user)))]
    (assoc state :result "Profile Updated")))

(defn registered-profiles [state _user]
  (if (:profile state) 1 0))

(defn search-by-name [state _user name-arg]
  (check (= _user (user)) "user arg and implicit user must be the same, for now")
  (if (-> state :profile :name (= name-arg))
    (:profile state)
    nil))