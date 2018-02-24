(defproject ezjava-980f "1.0.0-theriodontia" ;; theme: dinosaurs
  :description "EZJava: conveniences for writing crash-proof java programs"
  :url "https://github.com/980f/ezjava"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.jsoup/jsoup "1.10.2"]
    [mysql/mysql-connector-java "5.1.43"]
    [org.jetbrains/annotations "15.0"] ]
  :omit-source true
  :source-paths ["src"]
  :java-source-paths ["src"]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
