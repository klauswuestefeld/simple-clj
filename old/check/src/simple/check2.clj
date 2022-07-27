(ns simple.check2)

(defmacro check
  "Check if the given form is truthy, otherwise throw an exception with
  the given message. Alternative to `assert` that cannot be turned off
  and throwing RuntimeException instead of Error."
  [form otherwise-msg]
  `(or ~form
       (throw (new RuntimeException ~otherwise-msg))))
