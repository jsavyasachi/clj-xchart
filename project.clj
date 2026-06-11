(defproject com.hypirion/clj-xchart "0.3.0-SNAPSHOT"
  :description "XChart wrapper for Clojure"
  :url "https://github.com/hyPiRion/clj-xchart"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [org.knowm.xchart/xchart "4.0.1"]]
  :source-paths ["src/clj"]
  :global-vars {*warn-on-reflection* true}
  :plugins [[lein-codox "0.10.8"]]
  :deploy-repositories [["releases" :clojars]]
  :codox {:source-uri "https://github.com/hyPiRion/clj-xchart/blob/{version}/{filepath}#L{line}"}
  :profiles {:dev {:dependencies [[org.clojure/test.check "1.1.1"]]}
             :clojure-1-10 {:dependencies [[org.clojure/clojure "1.10.3"]]}
             :clojure-1-11 {:dependencies [[org.clojure/clojure "1.11.4"]]}
             :clojure-1-12 {:dependencies [[org.clojure/clojure "1.12.0"]]}}
  :aliases {"all" ["with-profile" "+clojure-1-10:+clojure-1-11:+clojure-1-12"]})
