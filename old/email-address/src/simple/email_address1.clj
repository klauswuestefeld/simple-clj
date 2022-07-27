(ns simple.email-address1)

(def email-regex #"(?i)[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?")

(defn email?
  "Returns address if it is a valid email, otherwise nil."
  [address] (and address (re-matches email-regex (str address))))
