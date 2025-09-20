(ns house.jux--.check--
  (:require
   [house.jux--.exceptions-- :refer [expected]]))

(defmacro check
  "Check if the given form is truthy, otherwise throw an exception with
  the given message. Alternative to `assert` that cannot be turned off
  and throwing RuntimeException instead of Error."
  [form & otherwise-msg-fragments]
  `(when-not ~form
       (throw (new RuntimeException (str ~@otherwise-msg-fragments)))))

#_{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(defmacro check-expected
  "Check if the given form is truthy, otherwise throw an 'expected'
  exception with the given message. Alternative to `assert` that
  cannot be turned off and throwing ex-info instead of Error."
  [form & otherwise-msg-fragments]
  `(when-not ~form
       (throw (expected (str ~@otherwise-msg-fragments)))))
