(ns house.jux--.prevayler4.git-coherence--
  (:require
   [clojure.java.shell :as shell]
   [clojure.java.io :as io]
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
      (throw (ex-info (str "Shell command exited with " (:exit result))
                      {:command args, :result result})))))

(defn- git [& args]
  (-> (apply run "git" args) .trim))

(defn- ns->path [sym]
  (-> sym name (str/replace "." "/") (str/replace "-" "_")))

(defn- restore-paths [{:keys [repo-dir src-dir]} prefix]
  (filter
   #(.exists (io/file repo-dir %))
   [(str src-dir "/" (ns->path prefix)) (str src-dir "/" (ns->path prefix) ".clj")]))

(defn- git-restore [required-commit {:keys [repo-dir src-dir refreshable-namespace-prefixes] :as opts}]
  (apply git "restore" "--source" required-commit "--staged" "--worktree" "--"
         (concat
          (mapcat (partial restore-paths opts) refreshable-namespace-prefixes)
          [:dir repo-dir])))

(defn- unload-deleted-namespaces [{:keys [repo-dir src-dir refreshable-namespace-prefixes]}]
  (let [existing-namespaces (-> (time (find-namespaces-in-dir (io/file repo-dir src-dir))) set)
        _ (check (seq existing-namespaces) "no namespaces were found")
        refreshable-namespace? (fn [namespace]
                                  (let [namespace-name (-> namespace ns-name name)]
                                    (some
                                     (fn [prefix]
                                       (str/starts-with? namespace-name (name prefix)))
                                     refreshable-namespace-prefixes)))
        deleted-namespace? (fn [namespace]
                             (not (contains? existing-namespaces (ns-name namespace))))
        candidates (->> (all-ns)
                        (filter refreshable-namespace?)
                        (filter deleted-namespace?))]
    (doseq [candidate candidates]
      (remove-ns (ns-name candidate)))))

(defn- load! [required-commit opts]
  (git-restore required-commit opts)
  (binding [*ns* (find-ns 'house.jux--.prevayler4.git-coherence--)] ; Any ns just to satisfy refresh's expectation of running in the repl.
    (let [r (repl/refresh)]
      (when (instance? java.lang.Throwable r)
        (throw r))))
  (unload-deleted-namespaces opts))

(defn commit-change-event [old-hash {:keys [repo-dir]}]
  (let [new-hash (git "rev-parse" "HEAD" :dir repo-dir)]
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
        (when-not (= required-commit @loaded-commit) ; Works for the first event also (loaded-commit atom is nil).
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
  (not (str/blank? (run "git" "status" "--porcelain"))))

(defn start!
  "Starts a prevayler instance that has git coherence enabled.

   Arguments:
   - start-prevayler-fn: a function that starts the underlying prevayler instance,
                         it must receive one argument containing the prevayler config
   - config: the prevayler config to be passed to the underlying prevayler instance
   - opts: a map with the following options
     - coherent-mode?: whether coherence should be enabled or not
     - git-reset?: whether the git workspace should be reset
     - repo-dir: an absolute java.io.File that points to the git repository directory,
                 defaults to the process working directory
     - src-dir: the path of the source directory as a string
     - refreshable-namespaces-prefixes: a sequence of namespace symbols that need to be refreshed.
                                        All namespaces whose names start with one of the given symbols
                                        will be refreshed (e.g. ['my-system.biz] will refresh 'my-system.biz,
                                        'my-system.biz.somenamespace and so on)"
  [start-prevayler-fn config {:keys [coherent-mode? git-reset? repo-dir src-dir] :as opts}]
  (let [opts (cond-> opts
               (nil? repo-dir) (assoc :repo-dir (.getAbsoluteFile (io/file ""))))]
    (when git-reset?
      (git "reset" "--hard" :dir repo-dir))
    (repl/set-refresh-dirs (io/file (:repo-dir opts) src-dir))
    (if coherent-mode?
      (check (not (workspace-dirty?)) "Unable to provide code coherence because workspace has uncommited files.")
      (println "COHERENCE IS OFF.\n  Journal replay might fail now or in future runs."))

    (let [new-prevayler (start-prevayler-fn (update config
                                                    :business-fn
                                                    wrap-for-code-version-coherence
                                                    opts))]
      (when coherent-mode?
        (when-let [event (commit-change-event (:current-commit-hash @new-prevayler) opts)]
          (prevayler/handle! new-prevayler event)))
      new-prevayler)))
