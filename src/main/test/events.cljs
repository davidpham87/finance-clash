(ns test.events
  (:require [re-frame.core :as rf :refer [reg-event-fx reg-event-db reg-fx]]
            ["react-navigation" :as rnav]
            ))

(rf/reg-event-db
 :toggle-drawer
 (fn [db _]
   (update-in db [:ui-states :drawer-open?] (fnil not 0))))

(reg-event-db
 :set-navigation
 (fn [db [_ nav]]
   (assoc db :navigator nav)))

(reg-fx
 :active-screen
 (fn [[navigator screen]]
   (.log js/console "Navigator" navigator "Screen" screen)
   (println "Navigator" navigator "Screen" screen)
   (when navigator
     (.dispatch navigator
                (.navigate rnav/NavigationActions #js {:routeName screen})))))

(reg-event-fx
 :register-active-screen
 (fn [{db :db} [_ screen params]]
   {:db (assoc db :active-screen screen
               :active-screen-params (if params params {}))}))

(reg-event-fx
 :set-active-screen
 (fn [{db :db} [_ active-screen]]
   {:db (assoc db :active-screen active-screen)
    :active-screen [(db :navigator) active-screen]}))


(reg-fx
 :navigation-drawer-action
 (fn [[navigator action]]
   (when navigator
     (.dispatch navigator
                (case action
                  :toggle (.toggleDrawer rnav/DrawerActions)
                  :open (.openDrawer rnav/DrawerActions)
                  :close (.closeDrawer rnav/DrawerActions))))))

(reg-event-fx
 :set-drawer-state
 (fn [{db :db} [_ action]]
   {:db db
    :navigation-drawer-action [(db :navigator) action]}))
