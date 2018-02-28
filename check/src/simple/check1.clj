(ns simple.check1)

(defn check [condition message-or-fn]
  (when-not condition
    (throw (new RuntimeException
                (if (string? message-or-fn)
                  message-or-fn
                  (message-or-fn))))))
