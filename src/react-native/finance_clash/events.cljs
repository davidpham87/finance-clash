(ns finance-clash.events
  (:require
   [re-frame.core :as rf :refer [reg-event-fx reg-event-db reg-fx]]
   [re-frame.db]
   ["react-navigation" :as rnav]))

(reg-event-db
 :initialize-db
 (fn [_ _] {}))

(reg-event-db
 :set-navigation
 (fn [db [_ nav]]
   (assoc db :navigator nav)))

(reg-fx
 :active-screen
 (fn [[navigator screen]]
   (.log js/console "Active-sccren FX: " screen)
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
    :active-screen [(db :navigator) (str screen-id)]}))


(reg-event-db
 :track-active-screen
 (fn [db [_ screen]]
   (assoc db :active-screen screen)))

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

(reg-event-db
 :register-platform
 (fn [db [_ plateform]]
   (assoc-in db [:expo :platform] plateform)))

(reg-event-db
 :record-window-dimensions
 (fn [db [_ {:keys [width height] :as m}]]
   (assoc db :window-dimensions m)))


(comment
 (def fetch (.-fetch js/window))
 (register-handler
  :load-data
  (fn [db _]
    (.then
     (fetch
      "[https://api.github.com/repositories](https://api.github.com/repositories)")
     #((.warn js/console
              (.stringify (.-JSON js/window) %1)
              (dispatch :process-data %1)))))))
