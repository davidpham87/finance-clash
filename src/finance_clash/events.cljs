(ns finance-clash.events
  (:require
   [re-frame.core :as rf :refer [reg-event-fx reg-event-db reg-fx]]
   ["react-navigation" :as rnav]))

(reg-event-db
 :set-navigation
 (fn [db [_ nav]]
   (assoc db :navigator nav)))

(reg-fx
 :active-screen
 (fn [[navigator screen]]
   (when navigator
     (.dispatch navigator
                (.navigate rnav/NavigationActions
                           #js {:routeName screen})))))

(reg-event-fx
 :register-active-screen
 (fn [{db :db} [_ screen params]]
   {:db (assoc db :active-screen screen
               :active-screen-params (if params params {}))}))

(reg-event-fx
 :set-active-screen
 (fn [{db :db} [_ screen-id]]
   {:db (assoc db :active-screen screen-id)
    :active-screen [(db :navigator) screen-id]}))

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

(reg-event-fx
 :toggle-drawer
 (fn [{db :db} [_ action]]
   {:db db
    :navigation-drawer-action [(db :navigator) :toggle]}))
