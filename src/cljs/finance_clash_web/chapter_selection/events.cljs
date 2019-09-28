(ns finance-clash-web.chapter-selection.events
  (:require
   [ajax.core :as ajax]
   [day8.re-frame.http-fx]
   [re-frame.core :as rf :refer (reg-event-db reg-event-fx)]))

(reg-event-fx
 ::record-next-series
 (fn [{db :db} [_ available-ids priority-ids]]
   {:db db
    :http-xhrio
    {:method :get
     :uri "http://localhost:3000"
     :format (ajax/json-request-format)
     :response-format (ajax/json-response-format {:keywords? true})
     :on-success [::success-record-next-series]}}))


(reg-event-fx
 ::success-record-next-series
 (fn [{db :db} [_ result]]
   {:db (update db :series-data (fnil conj []) resut)}))
