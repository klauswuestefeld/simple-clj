(ns house.jux--.tools.sampling-profiler--
  #_(:require [clojure.pprint :refer [pprint]]))

(def empty-summary {:sample-count 0})

(def summary (atom nil))
(def running (atom false))
(defn stop! []
  (reset! running false))


(defn- package-name [stack-frame]
  (let [fully-qualified-class (.getClassName stack-frame)
        last-dot (.lastIndexOf fully-qualified-class ".")]
    (if (pos? last-dot)
      (.substring fully-qualified-class 0 last-dot)
      nil)))

(defn- tally-package [summary package]
  (update-in summary [:package->count package] (fnil inc 0)))

(defn- tally-packages [summary stack]
  (->> (seq stack)
       (remove nil?)
       (map package-name)
       (reduce tally-package summary)))

(defn- summarize [summary thread->stack]
  (as-> summary _
    (update _ :sample-count inc)
    (reduce tally-packages _ (vals thread->stack))))

(defn- summarize-samples! [{:keys [duration-sec interval-ms]}]
  (let [end-time (+ (System/currentTimeMillis) (* 1000 duration-sec))]
    (loop []
      (Thread/sleep interval-ms)
      (swap! summary summarize (Thread/getAllStackTraces))
      (if (and @running
               (< (System/currentTimeMillis) end-time))
        (recur)
        (println "Profiling done.")))))

(defn start! [options]
  (when @running
    (throw (IllegalStateException. "Profiler was already running.")))
  (reset! running true)
  (reset! summary empty-summary)
  (future
    (try
      (summarize-samples! options)
      (catch Exception e
        (.printStackTrace e)))))


(comment
  ; Example usage: This will sample all thread stack frames every 20 milliseconds for a duration of 3 seconds. Useful for when the code being profiled hangs.
  (start! {:interval-ms 20
           :duration-sec 3})

  ; Run the code you want to profile...

  (stop!)  ; Stops profiling.
  @summary ; Contains the summary at any moment.
  )
