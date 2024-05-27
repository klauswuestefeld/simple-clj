(ns house.jux--.treated-check--)

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defmacro treated-check
  "Check if the given form is truthy, otherwise throw an exception with
  the given message and an attribute indicating it was already treated.
  Alternative to `assert` that cannot be turned off and throwing 
  RuntimeException instead of Error."
  [form & reason-message-fragments]
  `(or ~form
       (throw (ex-info (apply str ~@reason-message-fragments)
                       {:treated true}))))
