(ns finance-clash.server
  (:require
   [buddy.auth.backends.token]
   [buddy.auth.middleware]
   [finance-clash.budget]
   [finance-clash.db]
   [finance-clash.quiz]
   [finance-clash.specs]
   [finance-clash.user]
   [finance-clash.interceptors.cors :as cors]
   [muuntaja.core :as m]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]
   [reitit.http :as http]
   [reitit.http.coercion :as coercion]
   [reitit.http.interceptors.exception :as exception]
   [reitit.http.interceptors.muuntaja :as muuntaja]
   [reitit.http.interceptors.parameters :as parameters]
   [reitit.interceptor.sieppari :as sieppari]
   [reitit.ring :as ring]
   [ring.adapter.jetty :as jetty]
   [ring.middleware.cors]
   [ring.middleware.params :as params]))

(def secret "mysupersecret")
(def auth-backend (buddy.auth.backends.token/jws-backend
                   {:secret secret :options {:alg :hs512}}))

(def routes
  ["/plain"
   ["/plus" {:get (fn [{{:strs [x y]} :query-params :as req}]
                    {:status 200
                     :body {:total (+ (Long/parseLong x) (Long/parseLong y))}})
             :post (fn [{{:keys [x y]} :body-params}]
                     {:status 200
                      :body {:total (+ x y)}})}]])

(def app-data
  {:data
   {:muuntaja m/instance
    :access-control
    {:access-control-allow-origin
     [#"^(http(s)?://)?localhost:(\d){4}$"
      #"http://206.81.21.152"
      #"^(http(s)?://)?www.finance-clash-msiai.pro"]
     :access-control-allow-headers #{:accept :content-type :authorization}
     :access-control-allow-methods #{:get :put :post}}
    :interceptors
    [;; query-params & form-params
     (parameters/parameters-interceptor)
     ;; content-negotiation
     (muuntaja/format-negotiate-interceptor)
     ;; encoding response body
     (muuntaja/format-response-interceptor)
     ;; exception handling
     (exception/exception-interceptor)
     ;; decoding request body
     (muuntaja/format-request-interceptor)
     (coercion/coerce-request-interceptor)
     (coercion/coerce-response-interceptor)
     (cors/cors-interceptor)]}})

(def app-routes
  [["/" {:get (fn [request]
                (clojure.pprint/pprint
                 [["/" {:get (fn [request]
                               (clojure.pprint/pprint)
                               {:body "Hello from finance-clash server 1"})}]
                  ["/echo" {:get (fn [request] {:body "echo"})}]
                  routes
                  finance-clash.specs/routes
                  finance-clash.quiz/routes
                  finance-clash.user/routes
                  finance-clash.budget/routes])
                {:body "Hello from finance-clash server 3"})}]
   ["/echo" {:get (fn [request] {:body "echo"})}]
   routes
   finance-clash.specs/routes
   finance-clash.user/routes
   finance-clash.quiz/routes
   finance-clash.budget/routes])

(def app
  (-> (http/router app-routes app-data)
      (http/ring-handler (ring/create-default-handler)
                      {:executor sieppari/executor})
      (buddy.auth.middleware/wrap-authorization auth-backend)
      (buddy.auth.middleware/wrap-authentication auth-backend)))

(defonce server (atom nil))

(defn start []
  (let [jetty-server (jetty/run-jetty #'app {:port 3000, :join? false})]
    (reset! server jetty-server)
    (println "Server running in port 3000")))

(defn stop []
  (.stop @server))

(defn restart []
  (.stop @server)
  (println "Hello")
  (.start @server))

(comment
  (use '[clojure.tools.namespace.repl :only (refresh)])
  (start))
