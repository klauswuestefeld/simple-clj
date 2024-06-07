(ns simple.check2
  (:require
   [house.jux--.exceptions-- :refer [expected]]))

(defmacro check
  "Check if the given form is truthy, otherwise throw an exception with
  the given message. Alternative to `assert` that cannot be turned off
  and throwing RuntimeException instead of Error."
  [form & otherwise-msg-fragments]
  `(or ~form
       (throw (new RuntimeException (apply str ~@otherwise-msg-fragments)))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defmacro check-expected
  "Check if the given form is truthy, otherwise throw an 'expected'
  exception with the given message. Alternative to `assert` that
  cannot be turned off and throwing RuntimeException instead of Error."
  [form & otherwise-msg-fragments]
  `(or ~form
       (throw (expected (apply str ~@otherwise-msg-fragments)))))
