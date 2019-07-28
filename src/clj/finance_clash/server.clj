(ns finance-clash.server
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.params :as params]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [muuntaja.core :as m]
            [reitit.ring.coercion :as coercion]
            [reitit.ring :as ring]
            [finance-clash.specs]))

(def routes
  ["/plain"
   ["/plus" {:get (fn [{{:strs [x y]} :query-params :as req}]
                    {:status 200
                     :body {:total (+ (Long/parseLong x) (Long/parseLong y))}})
             :post (fn [{{:keys [x y]} :body-params}]
                     {:status 200
                      :body {:total (+ x y)}})}]])

(def app
  (ring/ring-handler
    (ring/router
     [routes
      finance-clash.specs/routes]
     {:data {:muuntaja m/instance
      	     :middleware
             [params/wrap-params
              muuntaja/format-middleware
              coercion/coerce-exceptions-middleware
              coercion/coerce-request-middleware
              coercion/coerce-response-middleware]}})
    (ring/create-default-handler)))

(defonce server (atom nil))

(defn start []
  (let [jetty-server (jetty/run-jetty #'app {:port 3000, :join? false})]
    (reset! server jetty-server)
    (println "server running in port 3000")))

(defn stop []
  (.stop @server))

(defn restart []
  (.stop @server)
  (.start @server))

(comment
  (use '[clojure.tools.namespace.repl :only (refresh)])
  (start))


