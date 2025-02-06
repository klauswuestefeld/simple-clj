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

(defn- git-restore [required-commit]
  (git "restore" "--source" required-commit "--staged" "--worktree" "--" "src"))

;; TODO it should support more than one namespace level
(defn- unload-deleted-namespaces [root-dir root-namespace-syms-set]
  (let [existing-namespaces (-> (find-namespaces-in-dir root-dir) set)
        candidates (->> (all-ns)
                        (filter (fn [namespace]
                                  (let [root-sym (-> namespace ns-name name (str/split #"\.") first symbol)]
                                    (contains? root-namespace-syms-set root-sym))))
                        (filter (fn [namespace]
                                  (not (contains? existing-namespaces (ns-name namespace))))))]
    (doseq [candidate candidates]
      (remove-ns (ns-name candidate)))))

(defn- load! [required-commit root-dir root-namespace-syms-set]
  (git-restore required-commit)
  (binding [*ns* (find-ns 'house.jux--.prevayler4.coherence--)] ; Any ns just to satisfy refresh's expectation of running in the repl.
    (repl/refresh))
  (unload-deleted-namespaces root-dir root-namespace-syms-set))

(defn commit-change-event [old-hash]
  (let [new-hash (git "rev-parse" "HEAD")]
    (when-not (= new-hash old-hash)
      {:current-commit-hash new-hash})))

(defn- ignoring-coherence [business-fn]
  (fn [state event timestamp]
    (if (:current-commit-hash event)
      state
      (business-fn state event timestamp))))

(defn- providing-coherence [business-fn root-dir root-namespace-syms-set]
  (let [loaded-commit (atom nil)]
    (fn [state event timestamp]
      (let [new-commit (:current-commit-hash event) ; Will be nil when event is a regular business event.
            state (if new-commit
                    (assoc state :current-commit-hash new-commit)
                    state)
            required-commit (:current-commit-hash state)]
        (check required-commit "Journal event must be replayed with an associated commit.")
        (when-not (= required-commit @loaded-commit) ; Works for the first event also (current-commit-atom is nil).
          (load! required-commit root-dir root-namespace-syms-set)
          (reset! loaded-commit required-commit))
        (if new-commit
          state
          (business-fn state event timestamp))))))

(defn- wrap-for-code-version-coherence [business-fn coherent-mode? root-dir root-namespace-syms-set]
  (if coherent-mode?
    (providing-coherence business-fn root-dir root-namespace-syms-set)
    (ignoring-coherence  business-fn)))

(defn- workspace-dirty? []
  (try
    (run "git" "diff" "--quiet")
    false
    (catch Exception e
      (if (-> e ex-data :result :exit (= 1))
        true
        (throw e)))))

(defn start! [start-prevayler-fn config root-dir root-namespace-syms-set]
  (let [coherent-mode? true]  ; TODO Allow override in dev environment
    (if coherent-mode?
      (check (not (workspace-dirty?)) "Unable to provide code coherence because workspace has uncommited files.")
      (println "COHERENCE IS OFF.\n  Journal replay might fail now or in future runs."))

    (let [new-prevayler (start-prevayler-fn (update config
                                                    :business-fn
                                                    wrap-for-code-version-coherence
                                                    coherent-mode?
                                                    root-dir
                                                    root-namespace-syms-set))]
      (when coherent-mode?
        (when-let [event (commit-change-event (:current-commit-hash @new-prevayler))]
          (prevayler/handle! new-prevayler event)))
      new-prevayler)))
