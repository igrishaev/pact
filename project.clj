(defproject com.github.igrishaev/pact "0.1.1-SNAPSHOT"

  :description
  "Chaining values with ease"

  :url
  "https://github.com/igrishaev/pact"

  :deploy-repositories
  {"releases" {:url "https://repo.clojars.org" :creds :gpg}}

  :license
  {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
   :url "https://www.eclipse.org/legal/epl-2.0/"}

  :release-tasks
  [["vcs" "assert-committed"]
   ["test"]
   ["change" "version" "leiningen.release/bump-version" "release"]
   ["vcs" "commit"]
   ["vcs" "tag" "--no-sign"]
   ["deploy"]
   ["change" "version" "leiningen.release/bump-version"]
   ["vcs" "commit"]
   ["vcs" "push"]]

  :dependencies
  []

  :profiles
  {:cljs
   {:cljsbuild
    {:builds
     [{:source-paths ["src" "test"]
       :compiler {:output-to "target/tests.js"
                  :output-dir "target"
                  :main pact.core-test
                  :target :nodejs}}]}

    :plugins
    [[lein-cljsbuild "1.1.8"]]

    :dependencies
    [[org.clojure/clojurescript "1.10.891"]
     [javax.xml.bind/jaxb-api "2.3.1"]
     [org.glassfish.jaxb/jaxb-runtime "2.3.1"]]}

   :dev
   {:dependencies
    [[org.clojure/clojure "1.10.1"]
     [manifold "0.1.9-alpha3"]
     [org.clojure/core.async "1.5.648"]]}})
