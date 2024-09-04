(ns house.jux--.biz.command-result--)

#_{:clj-kondo/ignore [:uninitialized-var]}
(def ^:dynamic *result*)

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn reset-result []
  (atom nil))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn get-result []
  (let [result @*result*]
    (if (fn? result)
      (result)
      result)))

(defn- capture-dynamic-bindings [v]
  (if (fn? v) (bound-fn [] (v)) v))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn set-result [v]
  (when (bound? #'*result*) ; This means some caller is interested in the result
    (reset! *result* (capture-dynamic-bindings v))))
