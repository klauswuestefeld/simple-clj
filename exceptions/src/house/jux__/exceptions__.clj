(ns house.jux--.exceptions--)

(def treated-message "treated-exception")

(defn throw-treated [& reason-message-fragments]
  (throw (ex-info treated-message
                  {:reason (apply str reason-message-fragments)})))
