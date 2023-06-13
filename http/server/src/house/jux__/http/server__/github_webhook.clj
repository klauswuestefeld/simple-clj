(ns house.jux--.http.server--.github-webhook
  (:require
   [cheshire.core :as json]
   [clojure.java.shell :as shell]))

(def secret (System/getenv "GITHUB_WEBHOOK_SECRET"))
(when-not secret (prn "GITHUB_WEBHOOK_SECRET env variable must be set"))

(defn- valid-webhook-request? [{:strs [uri params]}]
  (and
   (some->> secret (.endsWith uri))
   (let [parsed-payload (json/parse-string (get params "payload") keyword)]
     (= (get parsed-payload "ref")
        "refs/heads/main"))))

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