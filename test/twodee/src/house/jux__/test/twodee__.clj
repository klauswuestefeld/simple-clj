(ns house.jux--.test.twodee--
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as java.io]
            [clojure.string :as string]
            [simple.check2 :refer [check]]))

(def previous-query-results (atom nil)) ;; TODO: remove this atom
(def all-spreadsheets-folder (java.io/file "test/twodee"))
(def ^:dynamic *test-spreadsheet*)

(defn parse-csv [sheet-path]
  (let [reader (slurp sheet-path)]
    (csv/read-csv reader)))

(defn- parse-requires [require-sheet-path]
  (->> (parse-csv require-sheet-path)
       (into {})))

(defn- eval-require [namespace req ref]
  (let [req    (symbol req)
        refers (string/split ref #" ")
        refers (vec (map symbol refers))]
    (eval `(ns ~namespace (:require [~req :refer ~refers])))))

(defn- eval-requires [namespace requires]
  (doseq [[require refers] requires]
    (eval-require namespace require refers))
  (eval-require namespace "clojure.test" "is"))

(def query-line? (comp #{""} first))

(defn- queries [structure]
  (->> structure
       (filter query-line?)
       (map (partial drop 4))
       (apply map vector)))

;; (queries ns-tests)

(defn- initial-state-line [structure]
  (->> structure
       rest
       (remove query-line?)
       first))

;; (initial-state-line ns-tests)

(defn- initial-state [structure]
  (->> structure
       initial-state-line
       first))

(defn- initial-results [structure]
  (->> structure
       initial-state-line
       (drop 4)))

(defn- parse-query-results [provided-results]
  (let [result (vec (map-indexed (fn [idx provided-result]
                                   (if (string/blank? provided-result)
                                     (nth @previous-query-results idx)
                                     provided-result))
                                 provided-results))]
    (reset! previous-query-results result)
    result))

(defn- check-cell! [condition otherwise-msg coords]
  (when-not condition
    (let [error-map (assoc coords :spreadsheet *test-spreadsheet*)]
      (throw (ex-info otherwise-msg error-map)))))


(defn- step->map [starting-line line [user function params command-result & query-results]]
  (let [parsed-query-results (parse-query-results query-results)]
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
      (assoc-in [:command :function] "(fn [state & _ignored] state)")
      (assoc-in [:command :result] "*")))

(defn- steps [structure]
  (let [query-line-count (count (filter query-line? structure))
        starting-line    (inc query-line-count)
        raw-steps        (->> structure
                              (drop starting-line)
                              (map-indexed (partial step->map (inc starting-line)))
                              vec)]
    (update raw-steps 0 ->initial-step)))

(defn- check-blank-lines! [parsed-csv]
  (->> parsed-csv
       (map-indexed (fn [line-number values]
                      (check-cell! (not (string/blank? (apply str values)))
                                   "Please, remove blank line"
                                   {:column "A"
                                    :line   (inc line-number)})))
       dorun))

;; {:title "Sign in returns email, name and picture."
;;  :initial-state "{}"
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
  (check-blank-lines! parsed-csv)
  (let [title           (-> parsed-csv first first)
        queries         (queries parsed-csv)
        initial-state   (initial-state parsed-csv)
        _               (reset! previous-query-results (initial-results parsed-csv))
        steps           (steps parsed-csv)]
    {:title           title
     :initial-state   initial-state
     :queries         queries
     :steps           steps}))

(defn- execute-segment [value segment]
  ((read-string segment) value))

(defn- compare-results [{:keys [column line]} expected-result actual-result]
  (check-cell! (= (eval (read-string expected-result)) actual-result)
               (str "Actual result was:\n" (if (some? actual-result) actual-result "nil"))
               {:column column
                :line   line}))

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

(defn- exception->str [e]
  (or (.getMessage e) (str (.getClass e))))

(defn- query-result [state user [segment0 & segments]]
  (try
    (let [result0 (eval (list (symbol segment0) state (symbol user)))]
      (reduce execute-segment result0 (remove string/blank? segments)))
    (catch Exception e
      (exception->str e))))

(defn- execute-query [state query-results-line query-column-number [user & segments] expected-result]
  (let [actual-result (query-result state user segments)]
    (compare-results {:column (query-column-idx->column query-column-number)
                      :line   query-results-line}
                     expected-result
                     actual-result)))

(defn- check-queries! [state query-results-line queries results]
  (doseq [[idx [query result]] (->> results
                                    (map vector queries)
                                    (map-indexed vector))]
    (execute-query state query-results-line idx query result)))

(defn- check-command-result! [coords expected new-state]
  (check-cell! (not (string/blank? expected)) "Command result cannot be blank" coords)
  (check-cell! (some? new-state) "Command returned nil. Must return a state map." coords)
  (check-cell! (map? new-state) (str "Command returned a " (.getClass new-state) ". Must return a state map.") coords)
  (let [actual (:result new-state)]
    (when (and (instance? Exception actual) (not= expected "X"))
      (compare-results coords expected (exception->str actual)))
    (when-not (= expected "*")
      (check-cell! (contains? new-state :result) (str "Command returned the state without a :result key.") coords)
      (compare-results coords expected actual))))

(defn- resolve-fn [function line]
  (try
    (eval (read-string function))
    (catch Exception _e
      (check-cell! false (str "Unable to resolve function '" function "'") {:line line, :column "B"}))))

(defn- execute-command [state {:keys [function user params result result-coords]}]
  (let [function (resolve-fn function (:line result-coords))
        expression (if (string/blank? params)
                     (list function state (read-string user))
                     (list function state (read-string user) (read-string params)))
        new-state (try
                    (eval expression)
                    (catch Exception e
                      (assoc state :result e)))]
    (check-command-result! result-coords result new-state)
    new-state))

(defn- line [command]
  (-> command :result-coords :line))

(defn- execute-step [queries state {:keys [command query-results]}]
  (let [new-state (execute-command state command)]
    (check-queries! new-state (line command) queries query-results)
    new-state))

(defn- run-test [previous-state {:keys [initial-state queries steps]}]
  (let [initial-state (if (= initial-state "[parent]")
                        previous-state
                        (read-string initial-state))]
    (check-cell! (map? initial-state) "Initial state must be a map." {:line (-> steps first :command line)
                                                                      :column "A"})
    (reduce (partial execute-step queries) initial-state steps)))

(defn- require-namespace-refer-all [namespace required-namespace]
  (eval `(ns ~namespace (:require [~required-namespace :refer :all]))))

(defn- init-requires [subject-namespace all-requirements]
  (let [namespace 'tmp.twodees]
    (remove-ns namespace)
    (require-namespace-refer-all namespace (symbol subject-namespace))
    (doseq [requirements all-requirements]
      (eval-requires namespace requirements))))

(defn- run-test-in-file! [{:as context :keys [all-requirements state]} subject-namespace file]
  (binding [*ns* (find-ns 'house.jux--.test.twodee--)
            *test-spreadsheet* (.getName file)] ; TODO: find a cleaner way. This can be any ns just to set the root binding of *ns*
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

(defn- folder-test-files [files]
  (->> files
       (remove #(.isDirectory %))
       (remove #(= "require.csv" (.getName %)))
       (remove #(string/ends-with? % ".layout.edn"))))

(defn- requirements [folder]
  (let [requires-file (java.io/file folder "require.csv")]
    (if (.exists requires-file)
      (parse-requires requires-file)
      nil)))

(defn- run-tests-in-folder! [parent-context subject-namespace folder]
  (let [requirements (requirements folder)
        context      (cond-> parent-context
                       requirements (update :all-requirements (fnil conj []) requirements))
        children     (sorted-files folder)]
    (->> (folder-test-files children)
         (run! (fn [file]
                 (let [new-context (run-test-in-file! context subject-namespace file)]
                   (when-let [subfolder (corresponding-subfolder children file)]
                     (run-tests-in-folder! new-context subject-namespace subfolder))))))))

(defn- run-tests-in-namespace! [namespace-folder]
  (let [subject-namespace (-> namespace-folder .getName symbol)
        empty-context     nil]
    (run-tests-in-folder! empty-context subject-namespace namespace-folder)))

(defn- namespace-folders []
  (->> all-spreadsheets-folder sorted-files (filter #(.isDirectory %))))

(defn run-all-tests! []
  (run! run-tests-in-namespace! (namespace-folders))
  :OK)
