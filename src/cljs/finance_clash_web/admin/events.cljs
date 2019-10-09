(ns finance-clash-web.admin.events
  (:require
   [ajax.core :as ajax]
   [day8.re-frame.http-fx]
   [finance-clash-web.events :refer (endpoint auth-header)]
   [re-frame.core :refer (reg-event-fx)]))

(reg-event-fx
 ::update-password
 (fn [{db :db} [_ user-id password]]
   {:db db
    :http-xhrio {:method :put
                 :headers (auth-header db)
                 :uri (endpoint "user" user-id)
                 :params {:password password}
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [::success-update-password]
                 :on-failure [:api-request-error]}}))

(reg-event-fx
 ::success-update-password
 (fn [{db :db} [_ result]]
   (js/alert (str "Password of user " (:id result) " has been updated"))
   {:db db}))
