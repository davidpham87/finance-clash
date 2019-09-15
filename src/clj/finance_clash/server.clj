(ns finance-clash.server
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.params :as params]
            [ring.middleware.cors :refer [wrap-cors]]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [muuntaja.core :as m]
            [reitit.ring.coercion :as coercion]
            [reitit.ring :as ring]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [finance-clash.db]
            [finance-clash.specs]
            [finance-clash.user]
            [finance-clash.quizz]))

(def routes
  ["/plain"
   ["/plus" {:get (fn [{{:strs [x y]} :query-params :as req}]
                    {:status 200
                     :body {:total (+ (Long/parseLong x) (Long/parseLong y))}})
             :post (fn [{{:keys [x y]} :body-params}]
                     {:status 200
                      :body {:total (+ x y)}})}]])

(def app
  (wrap-cors
   (ring/ring-handler
    (ring/router
     [["/" {:get (fn [request] {:body "Hello from finance-clash server 2"})}]
      ["/echo" {:get (fn [request] {:body "echo"})}]
      routes
      finance-clash.specs/routes
      finance-clash.quizz/routes
      finance-clash.user/routes]
     {:data {:muuntaja m/instance
      	     :middleware
             [params/wrap-params
              muuntaja/format-middleware
              coercion/coerce-exceptions-middleware
              coercion/coerce-request-middleware
              coercion/coerce-response-middleware]}})
    (ring/create-default-handler))

   :access-control-allow-origin #"http://192.168.1.111:19006"
   :access-control-allow-headers #{:accept :content-type}
   :access-control-allow-methods #{:get :put :post}))

(defonce server (atom nil))

(defn start []
  (let [jetty-server (jetty/run-jetty #'app {:port 3000, :join? false})]
    (reset! server jetty-server)
    (println "server running in port 3000")))

(defn stop []
  (.stop @server))

(defn restart []
  (.stop @server)
  (println "Hello")
  (.start @server))

(comment
  (use '[clojure.tools.namespace.repl :only (refresh)])
  (start))
