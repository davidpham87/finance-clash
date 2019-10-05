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
   (println m)
   {:db (assoc-in db [:components :timer id] m)
    :dispatch [:register-interval
               {:id id :frequency 1000 :event [::update-timer id]}]}))

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
         v (dec (get-in db path))
         res {:db (assoc-in db path v)}]
     (println v)
     (if (pos? v)
       res
       (assoc res :dispatch [:clear-interval {:id id}])))))

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
 ::timer-remaining
 :<- [::timers]
 (fn [m [_ id]] (get-in m [id :remaining])))
