(ns house.jux--.http.server--.github-webhook
  (:require
   [cheshire.core :as json]
   [clojure.java.shell :as shell]))

(def secret (System/getenv "GITHUB_WEBHOOK_SECRET"))
(when-not secret (prn "GITHUB_WEBHOOK_SECRET env variable must be set"))

(defn- main-branch? [{:strs [payload]}]
  (-> payload
      json/parse-string
      (get "ref")
      (= "refs/heads/main")))

(defn- redeploy-if-necessary! [service-name on-redeploy params]
  (when (main-branch? params)
    (future
      (when (-> (shell/sh "git" "pull") :exit zero?)
        (on-redeploy)
        (shell/sh "systemctl" "restart" service-name))))
  {:status 202})

(defn wrap-redeploy! [delegate service-name on-redeploy]
  (fn [{:as request :keys [uri params]}]
    (if (some->> secret (.endsWith uri))
      (redeploy-if-necessary! service-name on-redeploy params)
      (delegate request))))