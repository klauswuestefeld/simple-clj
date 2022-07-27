(ns house.jux--.test.script--
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

(defn- passed? [{:keys [actual-result expected-result]}]
  (or (= actual-result expected-result)
      (exception-message? actual-result expected-result)))

(defn- throw-failure! [{:keys [description step# function user param expected-result actual-result]}]
  (println "Failed: " (yellow description))
  (println "Step" (str step# ": ") (yellow (demunge function)))
  (println "User:" user)
  (println "Param:" param)
  (println "Expected:" expected-result)
  (if (instance? Throwable actual-result)
    (do
      (println "  Actual:" (or (.getMessage actual-result) (-> actual-result .getClass .getSimpleName)))
      (write-exception *out* actual-result {:filter relevant-stack-frame?})
      (throw actual-result))
    (do
      (println "  Actual:" actual-result)
      (throw (RuntimeException. "Test failed")))))

(defn- step [test]
  (inc-pass-counter-if-using-midje)
  (-> test
      (select-keys [:single-user :description :state :step#])
      (update :step# inc)))

(defn- check-result! [test]
  (when-not (passed? test)
    (throw-failure! test))
  (step test))

(defn- arity [function]
  (-> function meta :arglists first count))

(defn- demunge [function]
  (-> function str repl/demunge (string/split #"@") first))
(defn- ->var [function]
  (-> function demunge symbol resolve))

(defn- process-command-result [test returned-result]
  (assoc test
         :state (dissoc returned-result :result)
         :actual-result (if (contains? returned-result :result)
                          (:result returned-result)
                          ::no-result)))

(defn- json-roundtrip [x]
  (-> x json/generate-string (json/parse-string keyword)))

(defn- args [{:keys [single-user state user param] :as test}]
  (let [user (or single-user user)]
    (if (contains? test :param)
      [state user (json-roundtrip param)]
      [state user])))

(defn- filter-bindings [param]
  (if (map? param)
    (->> param
         (filter (fn [[k _v]] (var? k)))
         (into {}))
    {}))

(defn- silent-apply [{:keys [function] :as test}]
  (try
    (let [returned-result (with-bindings (filter-bindings (:param test))
                            (apply function (args test)))]
      (if (-> function meta :command)
        (process-command-result test returned-result)
        (assoc test :actual-result returned-result)))
    (catch Exception e
      (assoc test :actual-result e))))

(defn- check-function [f]
  (check (fn? f) (str "Expected a function but got: " (if (nil? f) "nil" f))))


(defn- needs-user? [test]
  (and (not (:single-user test))
       (not (:user test))))

(defn- needs-function? [test]
  (not (:function test)))

(defn- needs-param? [test]
  (and (not (:param test))
       (-> test :function arity (= 3))))


(defn- try-to-check! [test]
  (if (-> test :actual-result (= ::no-result))
    (step test)
    test))

(defn- apply! [test]
  (-> test silent-apply try-to-check!))


(defn- handle-expected-result! [test expected]
  (-> test
      (assoc :expected-result expected)
      check-result!))

(defn- handle-param! [test param]
  (-> test
      (assoc :param param)
      apply!))

(defn- handle-function! [test f]
  (check-function f)
  (let [test (assoc test :function (->var f))]
    (if (needs-param? test)
      test
      (apply! test))))

(defn- handle-script-value! [test v]
  (cond
    (needs-user? test)
    (assoc test :user v)

    (needs-function? test)
    (handle-function! test v)

    (needs-param? test)
    (handle-param! test v)

    :else
    (handle-expected-result! test v)))

(defn- check-complete! [{:keys [user function description actual-result] :as test}]
  (when (instance? Exception actual-result)
    (throw actual-result))
  (check (and (not user)
              (not function))
         (str "The last step is incomplete in test: " description))
  test)

(defn script-user
  "Same as script, but takes an initial user argument, allowing the user to be
  omitted from the steps."
  [single-user description initial-state & script-values]
  (let [test {:single-user single-user
              :description description
              :state initial-state
              :step# 1}]
    (-> (reduce handle-script-value! test script-values)
        check-complete!
        :state)))

(defn script
  "Takes a test script description, its initial state and a sequence of steps.

  Each step is made of 3 or 4 values:
   - User
   - function to be called on the state
   - param (ommited if function does not take it)
   - Expected result

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
  [description initial-state & script-values]
  (apply script-user nil description initial-state script-values))
