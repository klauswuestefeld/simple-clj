(ns house.jux--.biz.timestamp--)

#_{:clj-kondo/ignore [:uninitialized-var]}
(def ^:dynamic *timestamp*)

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn reset-timestamp []
  (atom ::no-timestamp))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn get-timestamp []
  (let [timestamp @*timestamp*]
    (if (= timestamp ::no-timestamp)
      nil
      timestamp)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn set-timestamp [ts]
  (when (bound? #'*timestamp*)
    (reset! *timestamp* ts)))
