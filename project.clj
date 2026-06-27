(defproject net.clojars.savya/clj-xchart "0.3.4"
  :description "XChart wrapper for Clojure"
  :url "https://github.com/jsavyasachi/clj-xchart"
  :license {:name "Eclipse Public License 1.0"
            :url "https://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.12.5"]
                 [org.knowm.xchart/xchart "4.0.2"]]
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :javac-options ["-Xlint:unchecked"]
  :global-vars {*warn-on-reflection* true}
  :plugins [[lein-codox "0.10.8"]]
  :deploy-repositories [["releases" {:url "https://repo.clojars.org"
                                     :username :env/clojars_username
                                     :password :env/clojars_password
                                     :sign-releases false}]]
  :codox {:source-uri "https://github.com/jsavyasachi/clj-xchart/blob/{version}/{filepath}#L{line}"}
  :profiles {:dev {:dependencies [[org.clojure/test.check "1.1.3"]]}
             :clojure-1-10 {:dependencies [[org.clojure/clojure "1.10.3"]]}
             :clojure-1-11 {:dependencies [[org.clojure/clojure "1.11.4"]]}
             :clojure-1-12 {:dependencies [[org.clojure/clojure "1.12.5"]]}}
  :aliases {"all" ["with-profile" "+clojure-1-10:+clojure-1-11:+clojure-1-12"]})
