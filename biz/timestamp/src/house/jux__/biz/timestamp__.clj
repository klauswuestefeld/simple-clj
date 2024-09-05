(ns house.jux--.biz.timestamp--)

#_{:clj-kondo/ignore [:uninitialized-var]}
(def ^:dynamic *timestamp*)

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn timestamp []
  (when-not (bound? #'*timestamp*) (throw (IllegalStateException. "*timestamp* dynamic var should be bound")))
  *timestamp*)
