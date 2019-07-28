(defproject finance-clash "0.1.0-SNAPSHOT"
  :description "finance-clash backend"
  :dependencies [[cider/cider-nrepl "0.21.0"]
                 [org.clojure/clojure "1.10.0"]
                 [ring/ring-jetty-adapter "1.7.1"]
                 [metosin/reitit "0.3.9"]]
  :repl-options {:init-ns finance-clash.server}
  :plugins [[cider/cider-nrepl "0.21.1"]]
  :resource-paths ["assets"]
  :source-paths ["src/clj" "assets"])
