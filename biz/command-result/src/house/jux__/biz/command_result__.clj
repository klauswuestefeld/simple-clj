(ns house.jux--.biz.command-result--
  (:require [simple.check2 :refer [check]]))

#_{:clj-kondo/ignore [:uninitialized-var]}
(def ^:dynamic *result-atom*)

(defn new-result-atom []
  (atom ::no-result))

(defn peek-result []
  (let [result @*result-atom*]
    (if (= result ::no-result) nil result)))

(defn set-result-atom [v]
  (when (bound? #'*result-atom*)
    (check (= @*result-atom* ::no-result) (str "Transaction result was already set: " (if-some [result @*result-atom*] result "nil")))
    (reset! *result-atom* v)))
