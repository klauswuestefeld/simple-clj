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
            [babashka.process :as process]
            [clojure.string :as str]))

(def project-dir (io/file "test-project"))
(def repo-dir (io/file "test-repo"))

(def git #'coherence/git)

(defn commit! [message]
  (git "add" ".")
  (git "commit" "--no-gpg-sign" "-m" message))

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
    (process/destroy process)))

(defn server-url [server]
  (.url server))

(defn start-server! []
  (let [port (get-port)
        url (str "http://localhost:" port)
        process (process/process {:out :string :err :string :dir repo-dir} "clojure -M -m coherence-test.main" (str port) (str (fs/absolutize (fs/path repo-dir "src"))))]
    (if (wait/wait-for-port "localhost" port {:timeout 10000})
      (new Server url process)
      (do
        (when (process/alive? process)
          (process/destroy process))
        (throw (ex-info "failed to start server" {:process @process}))))))

(defn get-state [server]
  (-> @(http/get (str (server-url server) "/state"))
      :body
      io/reader
      (java.io.PushbackReader.)
      edn/read))

(defn post-command! [server command]
  (let [{:keys [status] :as response} @(http/post (str (server-url server) "/command") {:body (pr-str command)})]
    (when-not (= status 200)
      (throw (ex-info "failed to post command" {:response response})))))

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
      (fs/update-file (str (fs/path repo-dir "src/coherence_test/biz.clj"))
                      #(str/replace % #"\(def increment 0\)" "(def increment 1)"))
      (commit! "increment 1")
      (with-open [server (start-server!)]
        (post-command! server {:fn-sym 'coherence-test.biz/inc-event :args [1]})
        (is (= {:events [1 2]
                :current-commit-hash (current-hash)}
               (get-state server)))))
    (testing "it fails if workspace is dirty"
      (fs/update-file (str (fs/path repo-dir "src/coherence_test/biz.clj")) #(str % "; some change"))
      (try
        (start-server!)
        (is false "server should not have started")
        (catch clojure.lang.ExceptionInfo e
          (is (re-find #"Unable to provide code coherence because workspace has uncommited files" (-> e ex-data :process :err)))))
      (git "reset" "--hard" "HEAD"))
    (testing "it respects :current-commit-hash from snapshot"
      (with-open [server (start-server!)]
        (post-command! server {:fn-sym 'coherence-test.biz/inc-event :args [1]})
        (is (= {:events [1 2 2]
                :current-commit-hash (current-hash)}
               (get-state server))))
      (fs/update-file (str (fs/path repo-dir "src/coherence_test/biz.clj"))
                      #(str/replace % #"\(def increment 1\)" "(def increment 2)"))
      (commit! "increment 2")
      (with-open [server (start-server!)]
        (post-command! server {:fn-sym 'coherence-test.biz/inc-event :args [1]})
        (is (= {:events [1 2 2 3]
                :current-commit-hash (current-hash)}
               (get-state server)))))
    (testing "it respects file deletion"
      (fs/write-lines (fs/path repo-dir "src/coherence_test/tobe_deleted.clj")
                      ["(ns coherence-test.tobe-deleted)"
                       "(defn foo [state event] (assoc state :foo event))"])
      (commit! "add foobar")
      (with-open [server (start-server!)]
        (post-command! server {:fn-sym 'coherence-test.tobe-deleted/foo :args ["bar"]})
        (is (= {:events [1 2 2 3]
                :foo "bar"
                :current-commit-hash (current-hash)}
               (get-state server))))
      (fs/delete (fs/path repo-dir "src/coherence_test/tobe_deleted.clj" ))
      (commit! "delete foobar")
      (with-open [server (start-server!)]
        (post-command! server {:fn-sym 'coherence-test.biz/inc-event :args [1]})
        (is (= {:events [1 2 2 3 3]
                :foo "bar"
                :current-commit-hash (current-hash)}
               (get-state server)))
        (is (not (fs/exists? (fs/path repo-dir "src/coherence_test/tobe_deleted.clj"))))
        (is (thrown? clojure.lang.ExceptionInfo (post-command! server {:fn-sym 'coherence-test.tobe-deleted/foo :args ["bar"]})))))
    (testing "it respects refreshable namespaces"
      (fs/create-dir (fs/path repo-dir "src/non_refreshable"))
      (fs/write-lines (fs/path repo-dir "src/non_refreshable/foo.clj")
                      ["(ns non-refreshable.foo)"
                       "(defn foo [state event] (update state :foo1 (fnil conj []) event))"])
      (commit! "foobar reborn")
      (with-open [server (start-server!)]
        (post-command! server {:fn-sym 'non-refreshable.foo/foo :args ["bar1"]})
        (is (= {:events [1 2 2 3 3]
                :foo "bar"
                :foo1 ["bar1"]
                :current-commit-hash (current-hash)}
               (get-state server))))
      (fs/write-lines (fs/path repo-dir "src/non_refreshable/foo.clj")
                      ["(ns non-refreshable.foo)"
                       "(defn foo [state event] (update state :foo2 (fnil conj []) event))"])
      (commit! "foobar again")
      (with-open [server (start-server!)]
        (post-command! server {:fn-sym 'non-refreshable.foo/foo :args ["bar2"]})
        (is (= {:events [1 2 2 3 3]
                :foo "bar"
                :foo2 ["bar1" "bar2"]
                :current-commit-hash (current-hash)}
               (get-state server)))))))
