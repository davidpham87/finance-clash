(ns finance-clash-web.components.timer
  (:require
   [cljs-time.core :as ct]
   [cljs-time.format :as ctf]
   [reagent.core :as reagent]
   [re-frame.core :as rf :refer (reg-event-fx reg-sub)]))

(defn ->isoformat [d]
  (ctf/unparse (ctf/formatters :date-time) d))

(reg-event-fx
 ::start-timer
 (fn [{db :db} [_ {:keys [id duration start-time] :as m}]]
   (.log js/console m)
   (let [d (or (:start-time m) (ct/now))
         m (assoc m
                  :start-time d
                  :end-time (->isoformat (ct/plus d (ct/seconds (+ 2 duration))))
                  :remaining (+ 2 duration))]
     {:db (assoc-in db [:components :timer id] m)
      ;; :dispatch [:register-interval {:id id :frequency 1000
      ;;                                :event [::print id]}]
      })))

(reg-event-fx
 ::print
 (fn [{db :db} [_ id]]
   (.log js/console "Print")
   (.log js/console id)
   {:db db}))

(reg-event-fx
 ::update-timer
 (fn [{db :db} [_ id]]
   (let [path [:components :timer id :remaining]
         v (dec (get-in db path))]
     {:db (assoc-in db path v)
      :dispatch (if (pos? v) [:clear-interval {:id id}] [])})))

(reg-event-fx
 ::clear-timer
 (fn [db [_ id]]
   (let [path [:components :timer id]
         m (get-in db path)]
     {:db (assoc-in db path nil)
      :dispatch [:clear-interval {:id id}]})))

(reg-sub
 ::components
 (fn [db]
   (get-in db [:components])))

(reg-sub
 ::timers
 :<- [::components]
 (fn [comps]
   (get-in comps [:timer])))

(reg-sub
 ::timer
 :<- [::timers]
 (fn [m [_ id]] (get m id)))
