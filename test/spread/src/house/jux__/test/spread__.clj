(ns house.jux--.test.spread--
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as java.io]
            clojure.stacktrace
            [clojure.string :as string]
            clojure.walk
            [house.jux--.biz.command-result-- :refer [*result* get-result set-result reset-result]]
            [simple.check2 :refer [check]]))

(def all-spreadsheets-folder (java.io/file "test/spread"))
#_{:clj-kondo/ignore [:uninitialized-var]}
(def ^:dynamic *test-spreadsheet*)

(defn parse-csv [sheet-path]
  (let [reader (slurp sheet-path)]
    (csv/read-csv reader)))

(defn- parse-requires [require-sheet-path]
  (->> (parse-csv require-sheet-path)
       (into {})))

(defn- require-into-ns [namespace requires]
  (binding [*ns* (find-ns namespace)]
    (doseq [[req refers] requires]
      (let [req-sym (symbol req)]
        (require req-sym)
        (doseq [sym (map symbol (string/split refers #" "))]
          (refer req-sym :only [sym]))))
    (require 'clojure.test)
    (refer 'clojure.test :only ['is])))

(def first-column-blank? (comp #{""} first)) ; Description line and command lines do not have the first column blank.

(defn- queries [structure]
  (->> structure
       (filter first-column-blank?)
       drop-last                   ; The line with initial query values.
       (map (partial drop 4))
       (apply map vector)))

(defn- initial-results [structure]
  (->> structure
       (filter first-column-blank?)
       last
       (drop 4)))

(defn- parse-query-results [provided-results previous-results]
  (vec (map-indexed (fn [idx provided-result]
                      (if (string/blank? provided-result)
                        (nth previous-results idx)
                        provided-result))
                    provided-results)))

(defn- throw-info! [msg info]
  (throw (ex-info msg (assoc info :spreadsheet *test-spreadsheet*))))

(defn- check-info! [condition otherwise-msg info]
  (when-not condition
    (throw-info! otherwise-msg info)))

(defn- ->letter [idx]
  (->> idx (nth "ABCDEFGHIJKLMNOPQRSTUVWXYZ") str))

(defn- ->column [idx]
  (let [mostSignificantLetter  (if (-> idx (/ 26) int zero?)
                                 ""
                                 (-> idx (/ 26) int dec ->letter))
        leastSignificantLetter (->letter (mod idx 26))]
    (str mostSignificantLetter leastSignificantLetter)))

(defn- query-column-idx->column [idx]
  (->column (+ 4 idx)))

(defn- step->map [starting-line line previous-results [user function params command-result & query-results]]
  (let [parsed-query-results (parse-query-results query-results previous-results)]
    {:command {:user          user
               :function      function
               :params        params
               :result        command-result
               :result-coords {:line   (+ starting-line line)
                               :column "D"}}
     :query-results parsed-query-results}))

(defn- ->initial-step [step]
  (-> step
      (assoc-in [:command :user] "nil")
      (assoc-in [:command :function] "identity")
      (assoc-in [:command :result] "*")))

(defn- steps [structure initial-results]
  (let [starting-line (count (filter first-column-blank? structure))
        raw-steps        (->> structure
                              (drop starting-line)
                              (reduce (fn [{:as acc :keys [idx previous-results]} step-line]
                                        (let [step (step->map (inc starting-line) idx previous-results step-line)]
                                          (-> acc
                                              (update :steps (fnil conj []) step)
                                              (update :idx inc)
                                              (assoc :previous-results (:query-results step)))))
                                      {:previous-results initial-results
                                       :idx 0})
                              :steps)]
    (update raw-steps 0 ->initial-step)))

(defn- check-blanks! [parsed-csv]
  (check-info! (not (string/blank? (-> parsed-csv first first))) "The test needs a description" {:column "A", :line 1})
  (->> parsed-csv
       (map-indexed (fn [line-number values]
                      (check-info! (not (string/blank? (apply str values)))
                                   "Please, remove blank line"
                                   {:column "A"
                                    :line   (inc line-number)})))
       dorun))

;; {:title "Sign in returns email, name and picture."
;;  :queries [["ann" "existing-profile" ":email"]
;;            ["ann" "existing-profile" ":name"]
;;            ["ann" "existing-profile" ":given-name"]]
;;  :steps [{:command {:user "nil" :function nop :params "" :result "*"}
;;           :query-results ["nil" "nil" "nil"]}
;;          {:command {:user ":clock" :function "set-date" :params "\"2020-01-01\"" :result ""}
;;           :query-results ["" "" ""]}
;;          {:command {:user "ann" :function "sign-in" :params "{:name \"Ann A Smith\" :given-name \"Annabelle\" :family-name \"Smith\" :locale \"pt\"}" :result "*"}
;;           :query-results ["ann" "\"Ann A Smith\"" "\"Annabelle\""]}
;;          {:command {:user "ann" :function "sign-in" :params "{:name \"Annn\" :picture \"http://pics.com/ann\"}" :result "*"}
;;           :query-results ["" "" ""]}]}
(defn csv->test-map [parsed-csv]
  (check-blanks! parsed-csv)
  (let [title           (-> parsed-csv first first)
        queries         (queries parsed-csv)
        initial-results (initial-results parsed-csv)
        steps           (steps parsed-csv initial-results)]
    {:title           title
     :queries         queries
     :steps           steps}))

(defn- queries->cells [cols rows]
  (let [initial-col-idx 4
        initial-row     2]
    (->> (range initial-col-idx (+ cols initial-col-idx))
         (mapcat (fn [col-num]
                   (let [col-letter (->column col-num)]
                     (map (fn [row-num]
                            (str col-letter row-num))
                          (range initial-row (+ rows initial-row)))))))))

(defn- commands->cells [commands initial-command-row]
  (let [commands-cols 4
        commands-rows (- (count commands) 1) ;; initial step is not a user command
        ]
    (->> (range commands-cols)
         (mapcat (fn [col-num]
                   (let [col-letter (->column col-num)]
                     (map (fn [row-num]
                            (str col-letter row-num))
                          (range initial-command-row (+ commands-rows initial-command-row)))))))))

(defn- initial-results->cells [row cols]
  (let [inital-col-idx 4]
    (->> (range inital-col-idx (+ cols inital-col-idx))
         (map (fn [col-num]
                (str (->column col-num) row))))))

(defn cells-info [parsed-csv]
  (let [{:keys [queries steps]} (csv->test-map parsed-csv)
        queries-rows (apply max (map count queries))
        queries-cols (count queries)]
    {:queries         (queries->cells queries-cols queries-rows)
     :initial-results (initial-results->cells (+ queries-rows 2) queries-cols)
     :commands        (commands->cells steps (+ queries-rows 3))}))

(defn- exception->message [e]
  (or (.getMessage e) (str (.getClass e))))

(defn- check-exception! [{:as wrapper ::keys [wrapped-exception]} expected coords]

  (when (and (not= expected "X")
             (-> wrapped-exception .getMessage (not= expected)))
    (throw-info! (exception->message wrapped-exception)
                 (-> wrapper
                     (dissoc ::wrapped-exception)
                     (assoc :actual-exception wrapped-exception)
                     (merge coords)))))

(defn- deep-flatten [v]
  (tree-seq coll? seq v))

(defn- compile-string [s]
  (check (not (string/blank? s)) "String cannot be blank")
  (-> s read-string clojure.walk/macroexpand-all))

(defn- eval-string [s]
  (-> s compile-string eval))

(defn- prn-safe-for-eval [v]
  (->> v
       (clojure.walk/postwalk #(if (seq? %) (vec %) %))
       prn-str))

(defn- check-result! [actual expected coords]
  (let [expected (if (string/blank? expected) "\"<BLANK>\"" expected)]
    (if (::wrapped-exception actual)
      (check-exception! actual expected coords)
      (when-not (= expected "*")
        (let [ex-data (assoc coords :omit-stacktrace true)
              expected (if (= expected "X")
                         "X"
                         (try
                           (eval-string expected)
                           (catch Throwable e
                             (throw-info! (str "Error evaluating cell:\n" (exception->message e))
                                          ex-data))))]
          (when-not (= actual expected)
            (let [safe-actual (prn-safe-for-eval actual)]
              (throw-info! (str "Actual result was:\n" safe-actual)
                           (assoc ex-data :actual-value safe-actual)))))))))

(defn list-insert [lst elem index]
  (let [[l r] (split-at index lst)]
    (concat l [elem] r)))

(defn- replace-underscore [val form]
 (clojure.walk/postwalk
  (fn [x]
    (cond
      (= x '_) val
      (and (seq? x) (not (vector? x))) (apply list x)
      :else x))
  form))

(defn- with-underline [form current-result]
  (if (->> form deep-flatten (some #{'_}))
    (replace-underscore current-result form)
    (list-insert form current-result 1)))

(defn- resolve-symbol [arg]
  (if (symbol? arg)
   (let [arg (resolve arg)]
     @arg)
   arg))

(defn- resolve-user [user-string]
  (try
    (-> user-string read-string resolve-symbol)
    (catch Throwable t
      (throw (RuntimeException. (str "Error reading user: " (exception->message t)))))))

(defn- arity [function]
  (-> function meta :arglists first count))

(defn- run-fn [fn-var args ctx]
  (let [arity (arity fn-var)
        args  (if (= 3 arity)
                (cond-> args
                  (= 1 (count args)) (conj ::ignored ctx)
                  (= 2 (count args)) (conj ctx))
                args)]
    (apply fn-var args)))

(defn- run-query [ctx segment current-result]
  (let [safe-result (if (seq? current-result)
                      (vec current-result)
                      current-result)
        form (-> (str "(" segment ")")
                 compile-string
                 (with-underline safe-result))
        f      (first form)
        fn-var (if (keyword? f) f (resolve f))
        args   (eval (vec (rest form)))]
    (run-fn fn-var args ctx)))

(def initial-query-line 3)
(defn execute-query-segments
  ([state ctx segments]
   (execute-query-segments state ctx segments initial-query-line))

  ([value ctx [segment & next-segments] query-line]
   (if (string/blank? segment)
     value
     (try
       (let [result (run-query ctx segment value)]
         (execute-query-segments result ctx next-segments (inc query-line)))
       (catch Exception e
         {::wrapped-exception e
          :query-line query-line})))))

(defn- safe-query-result [state user segments]
  (execute-query-segments state {:user (resolve-user user) :timestamp 1000000} segments))

(defn- execute-query [state query-results-line query-column-number [user & segments] expected-result]
  (let [column (query-column-idx->column query-column-number)
        actual-result (safe-query-result state user segments)]
    (check-result! actual-result expected-result {:column column, :line query-results-line})))

(defn- check-queries! [state query-results-line queries results]
  (doseq [[idx [query result]] (->> results
                                    (map vector queries)
                                    (map-indexed vector))]
    (execute-query state query-results-line idx query result)))

(defn- check-command-result! [coords actual expected new-state]
  (check-info! (some? new-state) "Command returned nil. Must return a state map." coords)
  (check-info! (map? new-state) (str "Command returned a " (.getClass new-state) ". Must return a state map.") coords)
  (check-result! actual expected coords))

(defn- resolve-command-fn [function-str line]
  (try
    (->> function-str read-string resolve)
    (catch Exception e
      (throw-info! (str "Unable to resolve command '" function-str "' " (exception->message e))
                   {:line line, :column "B"}))))

(defn- execute-command [state {:keys [function user params result result-coords]}]
  (binding [*result* (reset-result)]
    (let [command   (resolve-command-fn function (:line result-coords))
          ctx       {:user (resolve-user user) :timestamp 1000000}
          args      (if (string/blank? params) [state] [state (eval-string params)])
          new-state (try
                      (run-fn command args ctx)
                      (catch Throwable e
                        (set-result {::wrapped-exception e})
                        state))]
      (check-command-result! result-coords (get-result) result new-state)
      new-state)))

(defn- line [command]
  (-> command :result-coords :line))

(defn- execute-step [queries state {:keys [command query-results]}]
  (let [new-state (execute-command state command)]
    (check-queries! new-state (line command) queries query-results)
    new-state))

(defn- run-test [state {:keys [queries steps]}]
  (reduce (partial execute-step queries) state steps))

(defn- require-namespace-refer-all [required-namespace]
  (let [req-sym (symbol required-namespace)]
    (require req-sym)
    (refer req-sym)
    (refer 'clojure.core))) ; namespaces created with create-ns dont have clojure.core refer already set-up

(defn- set-up-namespace [namespace]
  (remove-ns namespace)
  (create-ns namespace)
  (in-ns namespace))

(defn- init-requires [subject-namespace all-requirements]
  (let [namespace 'tmp.spreads]
    (set-up-namespace namespace)
    (require-namespace-refer-all subject-namespace)
    (doseq [requirements all-requirements]
      (require-into-ns namespace requirements))))

(defn- run-test-in-file! [{:as context :keys [all-requirements state]} subject-namespace file]
  (binding [*ns* (find-ns 'house.jux--.test.spread--)  ; TODO: find a cleaner way. This can be any ns just to set the root binding of *ns*
            *test-spreadsheet* (.getName file)]
    (let [relative-path       (.getPath file)
          test-map            (-> relative-path parse-csv csv->test-map)]
      (init-requires subject-namespace all-requirements)
      (assoc context :state (run-test state test-map)))))

(defn- sorted-files [directory]
  (->> directory .listFiles (sort-by #(.getName %))))

(defn- corresponding-subfolder [file-list file]
  (let [target-name (string/replace (.getName file) #".csv" "")]
    (->> file-list
         (filter #(= (.getName %) target-name))
         first)))

(def require-filename-suffix "require.csv")

(defn test-file? [file]
  (let [name (.getName file)]
    (and (string/ends-with? name ".csv")
         (not (.endsWith name require-filename-suffix)))))

(defn- file->specific-require-filename [file]
  (string/replace (.getName file) #".csv" (str "." require-filename-suffix)))

(defn- parse-requires-if-exists [file]
  (if (.exists file) (parse-requires file) nil))

(defn- specific-requirements [folder file]
  (let [requires-filename (file->specific-require-filename file)
        requires-file     (java.io/file folder requires-filename)]
    (parse-requires-if-exists requires-file)))

(defn- general-requirements [folder]
  (let [requires-file (java.io/file folder require-filename-suffix)]
    (parse-requires-if-exists requires-file)))

(defn- run-tests-in-folder! [parent-context subject-namespace folder]
  (let [requirements (general-requirements folder)
        context      (cond-> parent-context
                       requirements (update :all-requirements (fnil conj #{}) requirements))
        children     (sorted-files folder)]
    (->> children
         (filter test-file?)
         (run! (fn [file]
                 (let [specific-requirements (specific-requirements folder file)
                       context (cond-> context
                                 specific-requirements (update :all-requirements (fnil conj #{}) specific-requirements))
                       new-context (-> context
                                       (run-test-in-file! subject-namespace file)
                                       (update :all-requirements disj specific-requirements))]
                   (when-let [subfolder (corresponding-subfolder children file)]
                     (run-tests-in-folder! new-context subject-namespace subfolder))))))))

(defn- run-tests-in-namespace! [namespace-folder]
  (let [subject-namespace (.getName namespace-folder)
        empty-context     {:state {}}]
    (run-tests-in-folder! empty-context subject-namespace namespace-folder)))

(defn- namespace-folders []
  (->> all-spreadsheets-folder sorted-files (filter #(.isDirectory %))))

(defn run-all-tests! []
  (run! run-tests-in-namespace! (namespace-folders))
  :OK)
