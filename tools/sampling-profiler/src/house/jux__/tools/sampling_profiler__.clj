(ns house.jux--.tools.sampling-profiler--
  #_(:require [clojure.string :as str]))

(def empty-summary {:sample-count 0
                    :thread-sample-count 0})

(defonce summary-atom (atom nil))
(defonce running-atom (atom false))

(defn- tap [v] (doto v println))

(defn- ignore? [prefixes string]
  (or (nil? string)
      (some #(.startsWith string %) prefixes)))

(defn- tally-frame [{:keys [prefixes-to-ignore]}
                    status
                    summary
                    frame-str]
  (if (ignore? prefixes-to-ignore frame-str)
    summary
    (update-in summary [:frame->status->count frame-str status] (fnil inc 0))))

(defn- tally-frames [config summary stack]
  (let [summary (update summary :thread-sample-count inc)
        frame-strings (->> (seq stack)
                           (map #(.getClassName %))) ; TODO: Get method name too if class is not ignored.
        summary (tally-frame config :running summary (first frame-strings))]
    (reduce (partial tally-frame config :delegating)
            summary
            (rest frame-strings))))

(defn- summarize [summary thread-infos config]
  (as-> summary _
    (update _ :sample-count inc)
    (reduce (partial tally-frames config)
            _
            (map #(.getStackTrace %) thread-infos))))

(defn stop! []
  (reset! running-atom false)
  (println "Profiling stopped."))

(defn- get-thread-infos []
  (-> (java.lang.management.ManagementFactory/getThreadMXBean)
   (.dumpAllThreads false false))) ; More atomic than calling Thread/getAllStackTraces and then getState separately for each thread. Future: pass true as args to get lock info.

(defn- summarize-samples! [{:keys [duration-sec interval-ms config]}]
  (let [end-time (+ (System/currentTimeMillis) (* 1000 duration-sec))]
    (loop []
      (Thread/sleep interval-ms)
      (swap! summary-atom summarize (get-thread-infos) config)
      (if (and @running-atom
               (< (System/currentTimeMillis) end-time))
        (recur)
        (stop!)))))

(defn- unknown-tag [{:keys [prefixes-to-count]} frame]
  (if (ignore? prefixes-to-count frame)
    ""
    "[unknown] "))

(defn- impact [status->count]
  (get status->count :running 0))

(defn percent [part total]
  (let [percentage (with-precision 3
                     (-> (or part 0) (/ total) (* 100M)))
        formatted (format "%.1f%%" percentage)]
    (if (< (count formatted) 5)
      (str " " formatted)
      formatted)))

(defn- report-line [config thread-sample-count frame {:keys [running delegating]}]
  (str "  "     (percent running thread-sample-count)
       "      " (percent delegating thread-sample-count)
       "  "     (unknown-tag config frame)
       frame))

(defn- print-report [config]
  (let [summary @summary-atom
        thread-sample-count (:thread-sample-count summary)]
    (doseq [[frame status->count] (->> summary :frame->status->count (sort-by (comp impact second)))]
      (println (report-line config
                            thread-sample-count
                            frame
                            status->count))))
  (println "Running Delegating"))

(defn start! [options]
  (when @running-atom
    (throw (IllegalStateException. "Profiler is already running.")))
  (reset! running-atom true)
  (reset! summary-atom empty-summary)
  (future
    (try
      (summarize-samples! options)
      (catch Exception e
        (.printStackTrace e)
        (stop!)))))


(comment
  (def config {:prefixes-to-count ["house.jux__.tools"
                                   "java.lang.ref"
                                   "java.net"
                                   "java.util.concurrent"
                                   "java.util.concurrent.locks"
                                   "jdk.internal.misc"
                                   "jdk.internal.ref"
                                   "nrepl"
                                   "nrepl.middleware"
                                   "nrepl.transport"]
               :prefixes-to-ignore ["java.io"
                                    "java.lang"
                                    "nrepl.middleware"
                                    "nrepl.transport"
                                    "java.util.concurrent"
                                    "java.util.concurrent.locks"]})
  
  ; Example usage: This will sample all thread stack frames every 20 milliseconds for a maximum duration of 3 seconds. Useful for when the code being profiled hangs.
  (start! {:interval-ms 20
           :duration-sec 3
           :config config})

  ; Run the code you want to profile...
  
  (stop!)  ; Stops profiling.
  (print-report config))
