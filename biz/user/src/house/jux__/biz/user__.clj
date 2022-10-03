(ns house.jux--.biz.user--)

(def ^:dynamic *user*)

(defn user []
  (when-not (bound? #'*user*) (throw (IllegalStateException. "*user* dynamic var should be bound")))
  *user*)
  
