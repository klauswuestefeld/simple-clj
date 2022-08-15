(ns house.jux--.test.twodee--
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as java.io]
            [clojure.string :as string]
            [simple.check2 :refer [check]]))

(def previous-query-results (atom nil))
(def all-spreadsheets-folder (java.io/file "test/twodtest"))

(defn parse-csv [sheet-path]
  (let [reader (slurp sheet-path)]
    (csv/read-csv reader)))

(defn- require->refers [require-sheet-path]
  (->> (parse-csv require-sheet-path)
       (into {})))

(defn- eval-require [namespace req ref]
  (let [req    (symbol req)
        refers (string/split ref #" ")
        refers (vec (map symbol refers))]
    (eval `(ns ~namespace (:require [~req :refer ~refers])))))

(defn- eval-requires [namespace require-sheet-path]
  (doseq [[req ref] (require->refers require-sheet-path)]
    (eval-require namespace req ref))
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
    (throw (ex-info otherwise-msg coords))))

(defn- check-command-result! [result coords]
  (check-cell! (not (string/blank? result)) "Command result cannot be blank" coords))

;; (initial-results ns-tests)

(defn- step->map [starting-line line [user function params command-result & query-results]]
  (let [parsed-query-results (parse-query-results query-results)]
    {:command {:user          user
               :function      function
               :params        params
               :result        command-result
               :result-coords {:line   (+ starting-line line)
                               :column "D"}}
     :query-results parsed-query-results}))

(defn- steps [structure]
  (let [query-line-count (count (filter query-line? structure))
        starting-line    (+ query-line-count 2)]
    (->> structure
         (drop starting-line)
         (map-indexed (partial step->map (inc starting-line)))
         doall)))

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
;;  :initial-results ["nil" "nil" "nil"]
;;  :steps [{:command {:user ":clock" :function "set-date" :params "\"2020-01-01\"" :result ""}
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
        initial-results (initial-results parsed-csv)
        _               (reset! previous-query-results initial-results)
        steps           (steps parsed-csv)]
    {:title           title
     :initial-state   initial-state
     :queries         queries
     :initial-results initial-results
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

(defn- execute-query [state query-results-line query-column-number [user segment0 & segments] expected-result]
  (let [result0 (eval (list (symbol segment0) state (symbol user)))
        result  (if (string/blank? (first segments))
                  result0
                  (reduce execute-segment result0 segments))]
    (compare-results {:column (query-column-idx->column query-column-number)
                      :line   query-results-line} expected-result result)))

(defn- execute-queries [state query-results-line queries results]
  (doseq [[idx [query result]] (->> results
                                    (map vector queries)
                                    (map-indexed vector))]
    (execute-query state query-results-line idx query result)))

(defn- execute-command [state {:keys [function user params result result-coords]}]
  (let [new-state (if (string/blank? params)
                    (eval (list (symbol function) state (read-string user)))
                    (eval (list (symbol function) state (read-string user) (read-string params))))]
    (when-not (= result "*")
      (check-command-result! result result-coords)
      (check (contains? new-state :result) "Command did not return a result")
      (compare-results result-coords result (:result new-state)))
    new-state))

(defn- execute-step [queries state {:keys [command query-results]}]
  (let [new-state (execute-command state command)
        line      (-> command :result-coords :line)]
    (execute-queries new-state line queries query-results)
    new-state))

(defn- require-namespace-refer-all [namespace required-namespace]
  (eval `(ns ~namespace (:require [~required-namespace :refer :all]))))

(defn- init-requires [subject-namespace require-sheet-paths]
  (let [namespace 'tmp.twodtests]
    (remove-ns namespace)
    (require-namespace-refer-all namespace subject-namespace)
    (when-not (empty? require-sheet-paths)
      (doseq [require-sheet-path require-sheet-paths]
        (eval-requires namespace require-sheet-path)))))

(defn- run-test! [previous-result {:keys [initial-state initial-results queries steps]}]
  (let [initial-state (read-string initial-state)
        initial-state (if (= initial-state "[parent]")
                        previous-result
                        initial-state)
        initial-query-results-line (->> queries
                                        (map count)
                                        (apply max)
                                        (+ 2))]
    (execute-queries initial-state initial-query-results-line queries initial-results)
    (reduce (partial execute-step queries) initial-state steps)))

(defn- get-require-sheet-paths [test-path]
  (let [paths (-> test-path (string/split #"/") drop-last)]
    (->> paths
         (reduce (fn [acc cv]
                   (let [new-path (str (:path acc) (when-not (string/blank? cv)
                                                     "/") cv)
                         require-file (str new-path "/require.csv")
                         acc (assoc acc :path new-path)]
                     (if (-> require-file java.io/file .exists)
                       (update-in acc [:requires] conj require-file)
                       acc)))
                 {:path all-spreadsheets-folder
                  :requires []})
         :requires)))

(defn- path->namespace [path]
  (-> path
      (string/split #"/")
      (nth 2)))

(defn- ->test [path]
  {:test              (-> path parse-csv csv->test-map)
   :subject-namespace (path->namespace path)
   :relative-path     path})

(defn- set-up-test! [{:as state :keys [result]} {:keys [relative-path subject-namespace test]} ]
  (binding [*ns* (find-ns 'house.jux--.test.twodee--)] ; TODO: find a cleaner way. This can be any ns just to set the root binding of *ns*
    (let [require-sheet-paths (get-require-sheet-paths relative-path)
          test-namespace      (symbol subject-namespace)]
      (init-requires test-namespace require-sheet-paths)
      (assoc state :result (run-test! result test))
      (str "Test passed:" relative-path))))

(defn- init-test! [state file]
  (->> file
      .getPath
      ->test
      (set-up-test! state)))

(defn- sorted-files [directory]
  (->> directory .listFiles (sort-by #(.getName %))))

(defn- run-tests-in-folder! [state subject-namespace folder]
  (let [children (seq (sorted-files folder))]
    filtrar csv->test-map
    state-novo = (rodar state)
    se tiver dir c mesmo nome, fazer recursao com esse dir e state-novo
    #_(run! (fn [file]
              (if (.isDirectory file)
                (run-tests-in-folder! state subject-namespace file)
                (init-test! state file)))
            children)))

(defn- run-tests-in-namespace! [namespace-folder]
  (let [subject-namespace (resolve... namespace-folder)
        empty-state nil]
    (run! run-tests-in-folder! empty-state subject-namespace namespace-folder)
    
    #_(reduce (fn [state file]
              (if (.isDirectory file)
                (run-tests! state file)
                (init-test! state file))) 
            state
            children)))

(defn- namespace-folders []
  (->> all-spreadsheets-folder sorted-files (filter #(.isDirectory %))))

(defn run-all-tests! []
  (run! run-tests-in-namespace! (namespace-folders)))

;; Read all files:
;; path->test
;; {"appraise.biz/ann-signed-in"                   {:spreadsheet-data [["Ann signed in" ...]]}
;;  "appraise.biz/ann-signed-in/three-member-team" {:spreadsheet-data [[...]]}}

;; Parse test csv. Assoc keys to test:
;; {...
;;  :subject-namespace "appraise.biz"
;;  :test {...}}

;; Sort by path and run! Assoc result to test:
;; {...
;;  :result v}

;; Add path to ex-info
