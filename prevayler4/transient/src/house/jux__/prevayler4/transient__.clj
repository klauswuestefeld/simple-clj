(ns house.jux--.prevayler4.transient--
  (:require
   [prevayler-clj.prevayler4 :as prevayler]))

(defn counter []
  (let [counter-atom (atom 0)]
    #(swap! counter-atom inc)))

(defn prevayler! [{:keys [initial-state business-fn timestamp-fn]
                   :or {initial-state {}
                        timestamp-fn (counter)}}]
  (let [state-atom (atom initial-state)]
    (reify
      prevayler/Prevayler
      (handle! [this event]
        (locking this ; (I)solation: strict serializability.
          (let [timestamp (timestamp-fn)
                new-state (business-fn @state-atom event timestamp)] ; (C)onsistency: must be guaranteed by the business-fn.
            (reset! state-atom new-state)))) ; (A)tomicity
      (timestamp [_] (timestamp-fn))

      clojure.lang.IDeref (deref [_] @state-atom)

      java.io.Closeable (close [_] (reset! state-atom ::closed)))))