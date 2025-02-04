(ns prevayler4.coherence-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.shell :refer [with-sh-dir]]
            [clojure.java.io :as io]
            [house.jux--.prevayler4.coherence-- :as coherence]
            [clojure.tools.namespace.repl :as repl]
            [babashka.fs :as fs]
            [prevayler-clj.prevayler4 :as prev]))

(def repo-dir (io/file "coherence-test-repo"))
(def src-dir (io/file repo-dir "src"))
(def journal-file (io/file "coherence.journal4"))

(defn clean-repo-dir! []
  (fs/walk-file-tree
   repo-dir
   {:post-visit-dir (fn [path _]
                      (when-not (seq (fs/list-dir path))
                        (fs/delete path))
                      :continue)
    :visit-file (fn [path _]
                  (when-not (= ".donotdelete" (fs/file-name path))
                    (fs/delete path))
                  :continue)}))

(defn set-git-user []
  (#'coherence/git "config" "user.name" "John Doe")
  (#'coherence/git "config" "user.email" "john.doe@example.com"))

(defn commit! [files]
  (doseq [[file-name file-content] files]
    (let [file (io/file repo-dir file-name)]
      (fs/create-dirs (fs/parent file))
      (spit file file-content)))
  (#'coherence/git "add" ".")
  (#'coherence/git "commit" "--no-gpg-sign" "-m" "a commit"))

(defn start-prevayler []
  (coherence/start! prev/prevayler! {:business-fn (fn [state event timestamp]
                                                    (let [fun (find-var 'coherence-test.biz/my-business)]
                                                      (fun state event timestamp)))
                                     :initial-state {:events []}
                                     :journal-file journal-file}))

(deftest start!-test
  (clean-repo-dir!)
  (doseq [f (fs/list-dir (fs/cwd) "coherence.journal*")]
    (fs/delete f))
  (repl/set-refresh-dirs src-dir) ; it would be nice if we could undo this after the test

  (with-sh-dir repo-dir
    (#'coherence/git "init")
    (set-git-user)
    (commit! {"src/coherence_test/biz.clj" "(ns coherence-test.biz)\n(defn my-business [state event _] (update state :events conj event))\n"})
    (repl/refresh)
    (testing "it handles an event"
      (let [hash (#'coherence/git "rev-parse" "HEAD")
            prev (start-prevayler)]
        (prev/handle! prev 1)
        (is (= {:events [1]
                :current-commit-hash hash}
               @prev))))
    (testing "it replays the journal using previous code and handle event with new code"
      (commit! {"src/coherence_test/biz.clj" "(ns coherence-test.biz)\n(defn my-business [state event _] (update state :events conj (inc event)))\n"})
      (repl/refresh) ;; simulates a system restart with new code
      (let [hash (#'coherence/git "rev-parse" "HEAD")
            prev (start-prevayler)]
        (prev/handle! prev 1)
        (is (= {:events [1 2]
                :current-commit-hash hash}
               @prev))))
    (testing "it fails if workspace is dirty"
      (spit (io/file repo-dir "src/coherence_test/biz.clj")  "(ns coherence-test.biz)\n(defn my-business [state event _] (update state :events conj (inc event)) ; some change\n)\n")
      (is (thrown? RuntimeException #"Unable to provide code coherence"
                   (start-prevayler)))
      (#'coherence/git "reset" "--hard" "HEAD"))
    (testing "it starts with :current-commit-hash from snapshot"
      (let [hash (#'coherence/git "rev-parse" "HEAD")
            prev (start-prevayler)] ; forces a new snapshot
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
    (testing "it respects file deletion"
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
      (is (not (fs/exists? (io/file repo-dir "src/coherence_test/tobe_deleted.clj")))))))
