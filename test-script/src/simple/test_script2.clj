(ns simple.test-script2
  (:require
   [cheshire.core :as json]
   [io.aviso.exception :refer [write-exception]]
   [io.aviso.ansi :refer [yellow]]))

(def this-namespace (-> *ns* ns-name str))

(defn- relevant-stack-frame? [{:keys [package name]}]
  (cond
    ;(-> name    (.startsWith this-namespace)) :hide
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
  (-> function str clojure.repl/demunge (clojure.string/split #"@") first))

(defn- inc-pass-counter-if-using-midje []
  (when-let [inc-pass-counter (resolve 'midje.emission.state/output-counters:inc:midje-passes!)]
    (inc-pass-counter)))

(defn- command? [function]
  (-> function meta :command))

(defn- check-result [description step# function args result expected]
  (if (or (= result expected)
          (exception-message? result expected))
    (inc-pass-counter-if-using-midje)
    (do
      (println "Failed: " (yellow description))
      (println "Step" (str step# ": ") (yellow (demunge function)))
      (println "User:" (if (command? function)
                         (nth args 2) ; Skip the timestamp
                         (nth args 1)))
      (when (-> args count (= 4))
        (println "Args:" (nth args 3)))
      (println "Expected:" expected)
      (if (instance? Throwable result)
        (do
          (println "  Actual:" (or (.getMessage result) (-> result .getClass .getSimpleName)))
          (write-exception *out* result {:filter relevant-stack-frame?})
          (throw result))
        (do
          (println "  Actual:" result)
          (throw (RuntimeException. "Test failed")))))))

(defn- safe-apply [f args]
  (try
    (apply f args)
    (catch Exception result
      result)))

(defn- arity [function]
  (-> function meta :arglists first count))

(defn- ->var [function]
  (-> function demunge symbol resolve))

(defn- step [description state {:keys [step# function user params expected] :as step}]
  (let [command? (command? function)
        args (cond-> [state]
               command? (conj :fake-timestamp)
               true (conj user)
               (contains? step :params) (conj params))
        result (safe-apply function args)
        command-ok? (and command?
                         (not (instance? Exception result)))
        [state result] (if command-ok? result [state result])]
    (check-result description step# function args result expected)
    state))

(defn- json-roundtrip [x]
  (-> x json/generate-string (json/parse-string keyword)))

(defn- takes-params? [function]
  (if (command? function)
    (-> function arity (= 4))
    (-> function arity (= 3))))

(defn- ->steps [single-user script]
  (loop [result []
         step# 1
         script script]
    (let [user (or single-user (first script))
          script (cond-> script (not single-user) rest)
          [function & script] script]
      (if function
        (let [function (->var function)
              step {:step# step#, :function function, :user user}
              takes-params? (takes-params? function)
              step (cond-> step takes-params?
                           (assoc :params (-> script first json-roundtrip)))
              script (cond-> script takes-params? rest)
              [expected & script] script
              step (assoc step :expected expected)]
          (recur (conj result step)
                 (inc step#)
                 script))
        result))))

(defn script-user
  "Same as script, but takes an initial user argument, allowing the user to be
  omitted from the steps."
  [user description initial-state & script]
  (reduce (partial step description)
          initial-state
          (->steps user script)))

(defn script
  "Takes a test script description, its initial state and a sequence of steps.

  Each step, takes up 3 or 4 arguments:
   1) User
   2) function to be called on the state
   3) params map (ommited if function does not take one)
   4) Expected result

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
