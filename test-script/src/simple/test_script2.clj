(ns simple.test-script2
  (:require
   [clojure.repl :as repl]
   [clojure.string :as string]
   [cheshire.core :as json]
   [io.aviso.exception :refer [write-exception]]
   [io.aviso.ansi :refer [yellow]]
   [simple.check2 :refer [check]]))

(def this-namespace (-> *ns* ns-name str))

(defn- relevant-stack-frame? [{:keys [package name]}]
  (cond
    (-> name    (.startsWith this-namespace)) :hide
    (-> name    (.startsWith "midje.")) :hide
    (-> name    (.startsWith "clojure.")) :hide
    (-> package (.startsWith "clojure.")) :hide
    (-> package (.startsWith "java.")) :hide
    (-> package (.startsWith "sun.")) :hide
    :else :show))

(defn- exception-message? [result expected]
  (and
   (instance? Exception result)
   (.getMessage result)
   (instance? String expected)
   (-> result .getMessage (.contains expected))))

(defn- demunge [function]
  (-> function str repl/demunge (string/split #"@") first))

(defn- inc-pass-counter-if-using-midje []
  (when-let [inc-pass-counter (resolve 'midje.emission.state/output-counters:inc:midje-passes!)]
    (inc-pass-counter)))

(defn- passed? [{:keys [actual-result expected-result] :as test}]
  (or (not (contains? test :actual-result))
      (= actual-result expected-result)
      (exception-message? actual-result expected-result)))

(defn- check-result! [description step# {:keys [function user args expected-result actual-result] :as test}]
  (if (passed? test)
    (inc-pass-counter-if-using-midje)
    (do
      (println "Failed: " (yellow description))
      (println "Step" (str step# ": ") (yellow (demunge function)))
      (println "User:" user)
      (when (-> args count (= 3))
        (println "Params:" (nth args 2)))
      (println "Expected:" expected-result)
      (if (instance? Throwable actual-result)
        (do
          (println "  Actual:" (or (.getMessage actual-result) (-> actual-result .getClass .getSimpleName)))
          (write-exception *out* actual-result {:filter relevant-stack-frame?})
          (throw actual-result))
        (do
          (println "  Actual:" actual-result)
          (throw (RuntimeException. "Test failed")))))))

(defn- arity [function]
  (-> function meta :arglists first count))

(defn- ->var [function]
  (-> function demunge symbol resolve))

(defn- pop-expected-if-necessary [{:keys [script] :as test}]
  (cond-> test
    (contains? test :actual-result)
    (assoc :expected-result (first script)
           :script          (rest  script))))

(defn- process-command-result [test returned-result]
  (let [new-state (dissoc returned-result :result)
        test (assoc test :state new-state)]
    (if (contains? returned-result :result)
      (assoc test :actual-result (:result returned-result))
      test)))

(defn- silent-apply [{:keys [function args] :as test}]
  (try
    (let [returned-result (apply function args)]
      (if (-> function meta :command)
        (process-command-result test returned-result)
        (assoc test :actual-result returned-result)))
    (catch Exception e
      (assoc test :actual-result e))))

(defn- json-roundtrip [x]
  (-> x json/generate-string (json/parse-string keyword)))

(defn- pop-args [{:keys [function state user script] :as test}]
  (let [takes-params? (-> function arity (= 3))
        args   (cond-> [state user] takes-params? (conj (-> script first json-roundtrip)))
        script (cond-> script       takes-params? rest)]
    (assoc test :args args  
                :script script)))

(defn- pop-function [{:keys [script] :as test}]
  (let [function (first script)]
    (check (fn? function) (str "Expected a function but got: " (if (nil? function) "nil" function)))
    (assoc test :function (-> function ->var)
                :script   (-> script rest))))

(defn- pop-user [single-user {:keys [script] :as test}]
  (-> test
      (assoc :user   (or single-user        (first script))
             :script (if single-user script (rest  script)))))

(defn- step-script! [single-user description test]
  (loop [test test
         step# 1]
    (if (seq (:script test))
      (let [test (->> test
                      (pop-user single-user)
                      pop-function
                      pop-args
                      silent-apply
                      pop-expected-if-necessary)]
        (check-result! description step# test)
        (recur (select-keys test [:state :script])
               (inc step#)))
      (:state test))))

(defn script-user
  "Same as script, but takes an initial user argument, allowing the user to be
  omitted from the steps."
  [single-user description initial-state & script]
  (step-script! single-user description {:state initial-state
                                         :script script}))

(defn script
  "Takes a test script description, its initial state and a sequence of steps.

  Each step, takes up 3 or 4 arguments:
   1) User
   2) function to be called on the state
   3) params map (ommited if function does not take one)
   3 or 4) Expected result

  Executes the function in each step, checking for the expected result.
  If an exception is thrown, the expected result is compared to the exception message.
  The state returned by the function is passed on to the next step.

  Returns the state after the last step. This is useful for starting new scripts using
  the state created in previous ones.

  Example:
  (def initial-state {})
  (def with-users (script \"Users become active when they log in.\"
    inital-state
    :ann login nil
    :bob login nil
    :ann active-users [:ann :bob]))
  (script \"Users become inactive when they log out.\"
    with-users
    :bob logout nil
    :ann active-users [:ann])"
  [description initial-state & script]
  (apply script-user nil description initial-state script))

(defn ? [actual expected]
  ()
  actual)
(.getClass ?)