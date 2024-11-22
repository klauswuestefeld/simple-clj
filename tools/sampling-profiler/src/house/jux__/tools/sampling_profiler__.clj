(ns house.jux--.tools.sampling-profiler--
  (:require [clojure.string :refer [join]]
            [clojure.tools.namespace.repl]))

(defn- starts-with-any? [prefixes string]
  (some #(.startsWith string %) prefixes))

(defn- tally-frame [status summary frame]
  (if frame
    (update-in summary [:frame->status->count frame status] (fnil inc 0))
    summary))

(defn- relevant-frames [prefixes-to-ignore thread-info]
  (->> thread-info
       .getStackTrace
       seq
       (map #(.getClassName %))
       (remove nil?)
       (remove (partial starts-with-any? prefixes-to-ignore))))

(defn- conj-frame [status sample frame]
  (if (and status frame)
    (update sample status (fnil conj #{}) frame)
    sample))

(def state->blocked-status
  {Thread$State/BLOCKED :blocked-waiting       ; Waiting to enter a synchronized block/method.
   Thread$State/WAITING :blocked-waiting       ; Waiting due to calling one of the following methods: Object.wait() with no timeout, Thread.join with no timeout, LockSupport.park
   Thread$State/TIMED_WAITING :timed-waiting}) ; Sleeping or waiting with a timeout.

(println "TODO: List of frames to count as blocked because of waiting IO"
         "Detect critical threads (single threaded)")

(defn- conj-running-thread [sample thread-name state]
  (if (= state Thread$State/RUNNABLE)
    (update sample :running-threads conj thread-name)
    sample))

(defn- sample-thread [{:keys [prefixes-to-ignore]} sample thread-info]
  (let [stack (relevant-frames prefixes-to-ignore thread-info)
        state (.getThreadState thread-info)
        blocked-status (get state->blocked-status state)]
    (as-> sample _
      (conj-running-thread _ (.getThreadName thread-info) state)
      (conj-frame :running       _ (first stack))
      (conj-frame blocked-status _ (first stack))
      (reduce (partial conj-frame :delegating) _ (rest stack)))))

(defn- sample [config thread-infos]
  (reduce (partial sample-thread config) {} thread-infos))

(defn- tally-running-thread [summary thread-name]
  (update-in summary [:thread->running-frequency thread-name] (fnil inc 0)))

(defn- tally-sample [summary {:keys [running-threads running delegating blocked-waiting timed-waiting]}]
  (as-> summary _
    (update-in _ [:running-thread-count->frequency (count running-threads)] (fnil inc 0))
    (reduce tally-running-thread _ running-threads)
    (reduce (partial tally-frame         :running) _         running)
    (reduce (partial tally-frame      :delegating) _      delegating)
    (reduce (partial tally-frame :blocked-waiting) _ blocked-waiting)
    (reduce (partial tally-frame   :timed-waiting) _   timed-waiting)))

(defn- summarize [summary thread-infos config]
  (let [sample' (sample config thread-infos)]
    (-> summary
        (tally-sample sample')
        (update :sample-count inc))))

(defn stop! [session]
  (let [running-atom (:running-atom session)]
    (when @running-atom
      (reset! running-atom false)
      (println "Profiling stopped."))))

(defn- get-thread-infos []
  (-> (java.lang.management.ManagementFactory/getThreadMXBean)
      (.dumpAllThreads false false))) ; More atomic than calling Thread/getAllStackTraces and then getState separately for each thread. Future: pass true as args to get lock info.

(defn- summarize-samples! [{:as session :keys [duration-sec
                                               interval-ms
                                               config
                                               running-atom
                                               summary-atom]}]
  (let [end-time (+ (System/currentTimeMillis) (* 1000 duration-sec))]
    (loop []
      (Thread/sleep interval-ms)
      (swap! summary-atom summarize (get-thread-infos) config)
      (when @running-atom
        (if (< (System/currentTimeMillis) end-time)
          (recur)
          (stop! session))))))

(defn- unknown-tag [{:keys [prefixes-known]} frame]
  (if (starts-with-any? prefixes-known frame)
    " "
    "?"))

(defn- impact [status->count]
  [(get status->count :running 0)
   (get status->count :delegating 0)])

(defn- pad [size string]
  (if (< (count string) size)
    (recur size (str " " string))
    string))

(defn percent [part total]
  (->> (with-precision 4
         (-> (or part 0) (/ total) (* 100M)))
       (format "%.1f")))

(defn- waiting-tag [blocked-waiting timed-waiting]
  (if (or blocked-waiting timed-waiting) "*" " "))

(defn- waiting-info [sample-count blocked-waiting timed-waiting]
  (str (if (or blocked-waiting timed-waiting) "******* " "")
       (if blocked-waiting (str "BLOCKED: " (percent blocked-waiting sample-count)) "")
       "  "
       (if   timed-waiting (str "WAITING: " (percent   timed-waiting sample-count)) "")))

(defn- report-line [config sample-count frame {:keys [running delegating blocked-waiting timed-waiting]}]
  (str (pad 8  (percent running sample-count))
       (waiting-tag blocked-waiting timed-waiting)
       (pad 11 (percent delegating sample-count)) "  "
       (unknown-tag config frame) " "
       frame
       "  "
       (waiting-info sample-count blocked-waiting timed-waiting)))

(defn- significant? [significant-count status->count]
  (>= (apply + (impact status->count))
      significant-count))

(defn print-report [{:keys [config summary-atom]}]
  (let [summary @summary-atom
        sample-count (:sample-count summary)
        significant-count (* sample-count 0.01)]
    (doseq [[frame status->count] (->> summary :frame->status->count (sort-by (comp impact second)))]
      (when (significant? significant-count status->count)
        (println (report-line config
                              sample-count
                              frame
                              status->count))))
    (println "Running% Delegating%")
    (println (str "\nRunning thread count frequencies:\n"
                  (->> summary :running-thread-count->frequency sort (join "\n"))))
    (println (str "\nThread running frequencies:\n"
                  (->> summary :thread->running-frequency sort (join "\n"))))
    (println "\nEllapsed: " (-> (System/currentTimeMillis) (- (summary :start-millis)) (/ 1000M)) "seconds" (str "(" sample-count "samples)"))))

(defn start-session! [options]
  (let [session (assoc options
                       :summary-atom (atom {:sample-count 0
                                            :start-millis (System/currentTimeMillis)})
                       :running-atom (atom true))]
    (future
      (try
        (summarize-samples! session)
        (catch Exception e
          (.printStackTrace e)
          (stop! session))))
    session))


(comment 
 (def config {:prefixes-known ["bill_summary"
                                "financing"]
               :prefixes-to-ignore ["clojure"
                                    "java"
                                    "jdk"
                                    "nrepl"
                                    "sampling_profiler"
                                    "sun"
                                    "user$eval"
                                    ]})
  
  ; Example usage: This will sample all thread stack frames every 20 milliseconds for a maximum duration of N seconds (useful for when the code being profiled hangs).
  (let [session (start-session! {:interval-ms 20
                                 :duration-sec 14400 ; 4h
                                 :config config})]
    ; Run the code you want to profile...
    
    (stop! session) ; Stops profiling.
    (print-report session))
  )