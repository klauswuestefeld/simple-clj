(ns house.jux--.biz.user--)

#_{:clj-kondo/ignore [:uninitialized-var]}
(def ^:dynamic *user*)

(defn user []
  (when-not (bound? #'*user*) (throw (IllegalStateException. "*user* dynamic var should be bound")))
  *user*)
  
