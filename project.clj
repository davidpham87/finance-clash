(defproject finance-clash "0.1.0"
  :description "finance-clash backend"
  :repl-options {:init-ns finance-clash.server}
  :middleware [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn]
  :lein-tools-deps/config {:config-files [:install :user :project]
                           :aliases [:clj]}
  :ring {:handler finance-clash.server/app}
  :plugins [[cider/cider-nrepl "LATEST"]
            [lein-tools-deps "0.4.5"]
            [lein-ring "0.12.5"]]
  :resource-paths ["assets"]
  :source-paths ["src/clj" "assets"]
  :profile {:repl {:dependencies [[cider/cider-nrepl "LATEST"]
                                  [cider/piggieback "LATEST"]]}})
