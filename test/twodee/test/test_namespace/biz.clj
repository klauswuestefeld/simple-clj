(ns test-namespace.biz)

(defn create-new-profile [state user profile]
  (let [state (->> (assoc profile :created-by user)
                   (assoc state   :profile))]
    (assoc state :result "Profile Created")))

(defn created-profile [state _user]
  (:profile state))

(defn update-profile [state user profile]
  (let [state (-> state
                  (update :profile merge profile)
                  (assoc-in [:profile :last-updated-by] user))]
    (assoc state :result "Profile Updated")))
