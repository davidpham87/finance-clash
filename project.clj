(defproject finance-clash "0.1.0"
  :description "finance-clash backend"
  :dependencies [#_[cider/cider-nrepl "0.21.1"]
                 #_[cider/piggieback "0.4.1"]
                 [org.clojure/clojure "1.10.0"]
                 [ring/ring-jetty-adapter "1.7.1"]
                 [ring-cors "0.1.13"]
                 [metosin/reitit "0.3.9"]
                 [clj-http "3.10.0"]
                 [metosin/muuntaja-yaml "0.6.4"]]
  :repl-options {:init-ns finance-clash.server
                 :nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
  
  :plugins [#_[cider/cider-nrepl "0.21.1"]
            [lein-ring "0.12.5"]]
  :resource-paths ["assets"]
  :ring {:handler finance-clash.server/app}
  :source-paths ["src/clj" "assets"])
