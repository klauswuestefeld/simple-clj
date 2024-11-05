(ns house.jux--.tools.sampling-profiler--
  #_(:require [clojure.pprint :refer [pprint]]))

(def summary (atom nil))
(def running (atom false))
(defn stop! []
  (reset! running false))

(defn- tally-classes [summary stack]
  (->> (seq stack)
       (remove nil?)
       (map #(.getClassName %))
       (reduce (fn [summary class-name]
                 (update-in summary [:class->count class-name] (fnil inc 0)))
               summary)))

(defn- summarize [summary thread->stack]
  (as-> summary _
    (update _ :sample-count inc)
    (reduce tally-classes _ (vals thread->stack))))

(defn- summarize-samples! [{:keys [duration-sec interval-ms]}]
  (let [end-time (+ (System/currentTimeMillis) (* 1000 duration-sec))]
    (println "before loop")
    (loop []
      (println "looping")
      (Thread/sleep interval-ms)
      (swap! summary summarize (Thread/getAllStackTraces))
      (if (and @running
               (< (System/currentTimeMillis) end-time))
        (recur)
        (println "Profiling done.")))))

(defn start! [options]
  (when @running
    (throw (IllegalStateException. "Profiler was already running.")))
  (reset! summary {:sample-count 0})
  (reset! running true)
  (println "before future")
  (future
    (println "in future")
    (try
      (println "before summarize")

      (summarize-samples! options)
      (catch Exception e
        (.printStackTrace e)))))


(comment
  ; This will sample all thread stack frames every 20 milliseconds for a duration of 10 seconds. Useful for when the code being profiled hangs.
  (start! {:interval-ms 20
           :duration-sec 10})

  ; Run the code you want to profile...

  (stop!)  ; Stops profiling.
  @summary ; Contains the summary at any moment.
  )
