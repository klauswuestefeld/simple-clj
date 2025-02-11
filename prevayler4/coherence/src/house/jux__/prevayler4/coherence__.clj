(ns house.jux--.prevayler4.coherence--
  (:require
   [clojure.java.shell :as shell]
   [clojure.tools.namespace.repl :as repl]
   [clojure.tools.namespace.find :refer [find-namespaces-in-dir]]
   [prevayler-clj.prevayler4 :as prevayler]
   [simple.check2 :refer [check]]
   [clojure.string :as str]))

(defn- run [& args]
  (apply prn args)
  (let [result (apply shell/sh args)]
    (if (-> result :exit zero?)
      (:out result)
      (do
        (prn result)
        (throw (ex-info (str "Shell command exited with " (:exit result))
                        {:command args, :result result}))))))

(defn- git [& args] (-> (apply run "git" args) .trim))

(defn- ns->path [sym]
  (-> sym name (str/replace "." "/") (str/replace "-" "_")))

;; TODO use refreshable namespaces to define what to restore
(defn- git-restore [required-commit {:keys [src-dir refreshable-namespaces]}]
  (apply git "restore" "--source" required-commit "--staged" "--worktree" "--"
         (map (fn [sym]
                (format "%s/%s" (.getName src-dir) (ns->path sym)))
              refreshable-namespaces)))

(defn- unload-deleted-namespaces [{:keys [src-dir refreshable-namespaces]}]
  (let [existing-namespaces (-> (find-namespaces-in-dir src-dir) set)
        _ (check (seq existing-namespaces) "no namespaces were found")
        candidates (->> (all-ns)
                        (filter (fn [namespace]
                                  (let [namespace-name (-> namespace ns-name name)]
                                    (some
                                     (fn [refreshable-namespace]
                                       (str/starts-with? namespace-name (name refreshable-namespace)))
                                     refreshable-namespaces))))
                        (filter (fn [namespace]
                                  (not (contains? existing-namespaces (ns-name namespace))))))]
    (doseq [candidate candidates]
      (remove-ns (ns-name candidate)))))

(defn- load! [required-commit opts]
  (git-restore required-commit opts)
  (binding [*ns* (find-ns 'house.jux--.prevayler4.coherence--)] ; Any ns just to satisfy refresh's expectation of running in the repl.
    (repl/refresh))
  (unload-deleted-namespaces opts))

(defn commit-change-event [old-hash]
  (let [new-hash (git "rev-parse" "HEAD")]
    (when-not (= new-hash old-hash)
      {:current-commit-hash new-hash})))

(defn- ignoring-coherence [business-fn]
  (fn [state event timestamp]
    (if (:current-commit-hash event)
      state
      (business-fn state event timestamp))))

(defn- providing-coherence [business-fn opts]
  (let [loaded-commit (atom nil)]
    (fn [state event timestamp]
      (let [new-commit (:current-commit-hash event) ; Will be nil when event is a regular business event.
            state (if new-commit
                    (assoc state :current-commit-hash new-commit)
                    state)
            required-commit (:current-commit-hash state)]
        (check required-commit "Journal event must be replayed with an associated commit.")
        (when-not (= required-commit @loaded-commit) ; Works for the first event also (current-commit-atom is nil).
          (load! required-commit opts)
          (reset! loaded-commit required-commit))
        (if new-commit
          state
          (business-fn state event timestamp))))))

(defn- wrap-for-code-version-coherence [business-fn {:keys [coherent-mode?] :as opts}]
  (if coherent-mode?
    (providing-coherence business-fn opts)
    (ignoring-coherence  business-fn)))

(defn- workspace-dirty? []
  (try
    (run "git" "diff" "--quiet")
    false
    (catch Exception e
      (if (-> e ex-data :result :exit (= 1))
        true
        (throw e)))))

;; TODO trigger git reset when some property is on
(defn start! [start-prevayler-fn config {:keys [coherent-mode?] :as opts}]
  (if coherent-mode?
    (check (not (workspace-dirty?)) "Unable to provide code coherence because workspace has uncommited files.")
    (println "COHERENCE IS OFF.\n  Journal replay might fail now or in future runs."))

  (let [new-prevayler (start-prevayler-fn (update config
                                                  :business-fn
                                                  wrap-for-code-version-coherence
                                                  opts))]
    (when coherent-mode?
      (when-let [event (commit-change-event (:current-commit-hash @new-prevayler))]
        (prevayler/handle! new-prevayler event)))
    new-prevayler))
