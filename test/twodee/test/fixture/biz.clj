(ns fixture.biz)

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

(defn registered-profiles [state _user]
  (if (:profile state) 1 0))

(defn search-by-name [state _user name-arg]
  (if (-> state :profile :name (= name-arg))
    (:profile state)
    nil))