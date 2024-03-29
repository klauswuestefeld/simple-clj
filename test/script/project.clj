(defproject house.jux/test.script "2022.01.18"

  :description "Support for event driven test scripts."
  :url "https://github.com/klauswuestefeld/simple-clj/tree/master/test/script"

  :license {:name "BSD 3-Clause"
            :url "https://github.com/klauswuestefeld/simple-clj/blob/master/LICENSE"}

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [cheshire "5.8.0"] ; JSON for serializing test arguments.
                 [io.aviso/pretty "0.1.34"]
                 [simple/check "2.0.0"]])
