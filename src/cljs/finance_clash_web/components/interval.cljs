(ns finance-clash-web.components.interval
  (:require
   [re-frame.core :as rf :refer
    [reg-event-db reg-event-fx reg-fx inject-cofx trim-v after path debug]]))

(defonce interval-handler                ;; notice the use of defonce
  (let [live-intervals (atom {})]        ;; storage for live intervals
    (fn handler [{:keys [action id frequency event]}] ;; the effect handler
      (case action
        :clean   (doseq [k (keys @live-intervals)]
                   (handler {:action :end :id  k}))
        :start   (when-not (get @live-intervals id)
                   (swap! live-intervals assoc id
                          (js/setInterval #(rf/dispatch event) frequency)))
        :end     (do (js/clearInterval (get @live-intervals id))
                     (swap! live-intervals dissoc id))))))

;; when this code is reloaded `:clean` existing intervals
(interval-handler {:action :clean})

(reg-fx ;; the re-frame API for registering effect handlers
 :interval ;; the effect id
 interval-handler)

(reg-event-fx
 :clear-intervals
 (fn [_ _]
   {:interval {:action :clean}}))

(reg-event-fx
 :register-interval
 (fn [{db :db} [_ {:keys [id frequency event] :as m}]]
   {:db db
    :interval (assoc m :action :start)}))

(reg-event-fx
 :clear-interval
 (fn [{db :db} [_ {:keys [id] :as m}]]
   {:db db
    :interval (assoc m :action :end)}))
