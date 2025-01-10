(ns house.jux--.exceptions--)

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn message [e]
  (or (.getMessage e) (-> e .getClass str)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn expected [msg & [data]]
  (ex-info msg (assoc data :expected true)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn expected? [e]
  (:expected (ex-data e)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn insist! [operation f sleep-millis]
  (let [result-atom    (atom nil)
        exception-atom (atom nil)]
    (try
      (reset! result-atom (f))
      (catch Exception e
        (reset! exception-atom (message e))))
    (if-let [message @exception-atom]
      (do
        (println "Exception while" (str operation ":") message "\n Trying again...")
        (Thread/sleep sleep-millis)
        (recur operation f sleep-millis))
      @result-atom)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn ignoring-exception! [operation f]
  (try
    (f)
    (catch Exception e
      (println "Exception ignored while" (str operation ":") (message e)))))
