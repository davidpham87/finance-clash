(defproject finance-clash "0.1.0"
  :description "finance-clash backend"
  :dependencies
  [[cider/cider-nrepl "0.21.1"]
   [cider/piggieback "0.4.1"]
   [clj-http "3.10.0"]
   [com.layerware/hugsql "0.4.9"]
   [metosin/muuntaja-yaml "0.6.4"]
   [metosin/reitit "0.3.9" :excluesions [metosin/jsonista]]
   [org.clojure/clojure "1.10.0"]
   [ring-cors "0.1.13"]
   [ring/ring-jetty-adapter "1.7.1"]
   [seancorfield/next.jdbc "1.0.5" :exclusions [org.clojure/clojure]]
   [org.xerial/sqlite-jdbc "3.28.0"]

   [re-frisk "0.5.3"]
   ;; [javax.xml.bind/jaxb-api "2.3.0"]
   ;; [com.sun.xml.bind/jaxb-core "2.3.0"]
   ;; [com.sun.xml.bind/jaxb-impl "2.3.0"]
   ]

  :repl-options {:init-ns finance-clash.server}
  :ring {:handler finance-clash.server/app}
  :plugins [[cider/cider-nrepl "0.21.1"]
            [lein-re-frisk "0.5.8"]
            [lein-ring "0.12.5"]]
  :resource-paths ["assets"]
  :source-paths ["src/clj" "assets"]

  )
