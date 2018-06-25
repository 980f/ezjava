(defproject ezjava-980f "0.1.0-SNAPSHOT"
  :description "EZJava: conveniences for writing crash-proof java programs"
  :url "https://github.com/980f/ezjava"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [
    [mysql/mysql-connector-java "5.1.46"]
    [org.jsoup/jsoup "1.11.3"]
    [org.jetbrains/annotations "15.0"]
  ]
  :omit-source true
  :java-source-paths ["src"]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
