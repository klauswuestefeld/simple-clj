(ns house.jux--.http.github-webhook--
  (:require
   [cheshire.core :as json]
   [clojure.java.shell :as shell]))

(def secret (System/getenv "GITHUB_WEBHOOK_SECRET"))
(when-not secret (prn "GITHUB_WEBHOOK_SECRET env variable must be set"))

(defn- valid-webhook-request? [request]
  (and
   (some->> secret (.endsWith (:uri request)))
   (-> request :params :payload (json/parse-string keyword) :ref
       (= "refs/heads/main"))))

(defn- redeploy! [service-name on-redeploy]
  (future
    (when (-> (shell/sh "git" "pull") :exit zero?)
      (on-redeploy)
      (shell/sh "systemctl" "restart" service-name)))
  {:status 202})

(defn wrap-redeploy! [delegate service-name on-redeploy]
  (fn [request]
    (if (valid-webhook-request? request)
      (redeploy! service-name on-redeploy)
      (delegate request))))