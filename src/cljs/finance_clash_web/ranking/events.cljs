(ns finance-clash-web.ranking.events
  (:require
   [ajax.core :as ajax]
   [day8.re-frame.http-fx]
   [finance-clash-web.events :refer (endpoint)]
   [re-frame.core :refer (reg-event-fx)]))

(reg-event-fx
 ::query-ranking
 (fn [{db :db} _]
   {:db db
    :http-xhrio
    {:method :get
     :uri (endpoint "ranking")
     :format (ajax/json-request-format)
     :response (ajax/json-response-format)
     :on-success [::success-query-ranking]
     :on-failure [:api-request-error]}}))

(reg-event-fx
 ::success-query-ranking
 (fn [{db :db} [_ result]]
   {:db (assoc db :ranking result)}))
