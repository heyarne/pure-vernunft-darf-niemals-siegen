(defproject ai-for-games "0.1.0-SNAPSHOT"
  :description "Pure Vernunft Darf Niemals Siegen"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.cli "0.3.7"]]
  :main ^:skip-aot ai-for-games.cli
  :target-path "target/%s"
  ;; add provided game server + classes to class path
  :jvm-opts ["-Djava.library.path=resources/gawihs/lib/native"]
  :resource-paths ["resources/gawihs/gawihs.jar"]
  :profiles {:uberjar {:aot :all}})
