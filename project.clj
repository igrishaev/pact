(defproject pact "0.1.0-SNAPSHOT"

  :description
  "FIXME: write description"

  :url
  "http://example.com/FIXME"

  :license
  {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
   :url "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies []

  :profiles
  {:dev
   {:dependencies
    [[org.clojure/clojure "1.10.1"]
     [manifold "0.1.9-alpha3"]
     [org.clojure/core.async "1.5.648"]]}

   :uberjar
   {:aot :all
    :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
