(ns coherence-test.biz)

(def increment 1)

(defn inc-event [state event]
  (update state :events (fnil conj []) (+ event increment)))
