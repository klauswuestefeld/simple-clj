(ns coherence-test.biz)
(defn my-business [state event _] (update state :events conj (inc (inc event))))
