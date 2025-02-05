(ns coherence-test.biz)

(def increment 0)

(defn inc-event [state event]
  (update state :events (fnil conj []) (+ event increment)))
