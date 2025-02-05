(ns prevayler4.coherence-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.shell :refer [with-sh-dir]]
            [clojure.java.io :as io]
            [house.jux--.prevayler4.coherence-- :as coherence]
            [babashka.fs :as fs]
            [org.httpkit.client :as http]
            [clojure.edn :as edn]
            [prestancedesign.get-port :refer [get-port]]
            [babashka.wait :as wait]
            [clojure.java.process :as process]
            [clojure.string :as str]))

(def project-dir (io/file "test-project"))
(def repo-dir (io/file "test-repo"))

(def git #'coherence/git)

(defn commit! [message]
  (git "add" ".")
  (git "commit" "--no-gpg-sign" "-m" message))

(defn modify-file! [path modify-fn]
  (let [file (io/file repo-dir path)
        content (slurp file)]
    (spit file (modify-fn content))))

(defn setup-repo-dir! []
  (fs/delete-tree repo-dir)
  (fs/create-dir repo-dir)
  (fs/copy-tree project-dir repo-dir)
  (git "init")
  (git "config" "user.name" "John Doe")
  (git "config" "user.email" "john.doe@example.com")
  (commit! "initial commit"))

(defn current-hash []
  (git "rev-parse" "HEAD"))

(deftype Server [url process]
  java.lang.AutoCloseable
  (close [_]
    (.destroy process)))

(defn server-url [server]
  (.url server))

(defn start-server! []
  (let [port (get-port)
        url (str "http://localhost:" port)
        process (process/start {:dir repo-dir} "clojure" "-M" "-m" "coherence-test.main" (str port))]
    (if (wait/wait-for-port "localhost" port {:timeout 5000})
      (new Server url process)
      (throw (ex-info "failed to start server" {:process #tap process})))))

(defn get-state [server]
  (-> @(http/get (str (server-url server) "/state"))
      :body
      io/reader
      (java.io.PushbackReader.)
      edn/read))

(defn post-command! [server command]
  @(http/post (str (server-url server) "/command") {:body (pr-str command)}))

(deftest start!-test
  (with-sh-dir repo-dir
    (setup-repo-dir!)
    (testing "it handles an event"
      (with-open [server (start-server!)]
        (post-command! server {:fn-sym 'coherence-test.biz/inc-event :args [1]})
        (is (= {:events [1]
                :current-commit-hash (current-hash)}
               (get-state server)))))
    (testing "it replays the journal using previous code and handle event with new code"
      (modify-file! "src/coherence_test/biz.clj" #(str/replace % #"\(def increment 0\)" "(def increment 1)"))
      (commit! "increment 1")
      (with-open [server (start-server!)]
        (post-command! server {:fn-sym 'coherence-test.biz/inc-event :args [1]})
        (is (= {:events [1 2]
                :current-commit-hash (current-hash)}
               (get-state server)))))
    (testing "it fails if workspace is dirty"
      (modify-file! "src/coherence_test/biz.clj" #(str % "; some change"))
      (is (thrown? clojure.lang.ExceptionInfo #"failed to start server" (start-server!)))
      (#'coherence/git "reset" "--hard" "HEAD"))
    #_(testing "it respects :current-commit-hash from snapshot"
      (let [hash (#'coherence/git "rev-parse" "HEAD")
            prev (start-prevayler)]     ; forces a new snapshot
        (prev/handle! prev 1)
        (is (= {:events [1 2 2]
                :current-commit-hash hash}
               @prev)))
      (commit! {"src/coherence_test/biz.clj" "(ns coherence-test.biz)\n(defn my-business [state event _] (update state :events conj (inc (inc event))))\n"})
      (repl/refresh) ;; simulates a system restart with new code
      (let [hash (#'coherence/git "rev-parse" "HEAD")
            prev (start-prevayler)]
        (prev/handle! prev 1)
        (is (= {:events [1 2 2 3]
                :current-commit-hash hash}
               @prev))))
    #_(testing "it respects file deletion"
      (commit! {"src/coherence_test/tobe_deleted.clj" "(ns coherence-test.tobe-deleted)\n(defn foo [] \"bar\")\n"})
      (repl/refresh)
      (is (= "bar" (apply (find-var 'coherence-test.tobe-deleted/foo) [])))
      (start-prevayler) ;; adds new commit to journal
      ;; TODO add an event that exercises the deleted namespace
      (fs/delete (io/file repo-dir "src/coherence_test/tobe_deleted.clj" ))
      (is (not (fs/exists? (io/file repo-dir "src/coherence_test/tobe_deleted.clj"))))
      (#'coherence/git "add" ".")
      (#'coherence/git "commit" "--no-gpg-sign" "-m" "a commit")
      (repl/refresh)
      (start-prevayler)
      ;; TODO unload deleted namespace
      (is (not (fs/exists? (io/file repo-dir "src/coherence_test/tobe_deleted.clj")))))
    #_(testing "it supports more than one workspace dir" ;; TODO
      )))
