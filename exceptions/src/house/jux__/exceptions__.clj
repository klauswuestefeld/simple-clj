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
