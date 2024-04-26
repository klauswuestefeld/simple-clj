(ns house.jux--.biz.command-result--
  (:require [simple.check2 :refer [check]]))

#_{:clj-kondo/ignore [:uninitialized-var]}
(def ^:dynamic *result*)

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn reset-result []
  (atom ::no-result))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn get-result []
  (let [result @*result*]
    (if (= result ::no-result) nil result)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn set-result [v]
  (when (bound? #'*result*)
    (check (= @*result* ::no-result) (str "Transaction result was already set: " (if-some [result @*result*] result "nil")))
    (reset! *result* v)))
