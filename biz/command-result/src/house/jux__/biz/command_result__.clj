(ns house.jux--.biz.command-result--
  (:require [simple.check2 :refer [check]]))

#_{:clj-kondo/ignore [:uninitialized-var]}
(def ^:dynamic *result*)

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn reset-result []
  (atom ::no-result))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn voucher []
  (let [voucher @*result*]
    (if  (= ::no-result voucher)
      nil
      voucher)))

(defn- create-voucher [v]
  {::bindings (or (get-thread-bindings) {})
   ::value    v})

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn set-result [v]
  (when (bound? #'*result*) ; This means some caller is interested in the result
    (check (= @*result* ::no-result) (str "Transaction result was already set: " (if-some [result @*result*] result "nil")))
    (reset! *result* (create-voucher v))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn redeem [f voucher]
  (if-let [bindings (::bindings voucher)]
    (with-bindings bindings
      (let [v (::value voucher)]
        (f (if (fn? v) (v) v))))
    (f voucher)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn pair [f]
  (binding [*result* (reset-result)]
    [(f) (redeem identity (voucher))]))
