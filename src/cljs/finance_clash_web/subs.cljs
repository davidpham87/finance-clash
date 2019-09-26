(ns finance-clash-web.subs
  (:require
   [re-frame.core :as rf :refer [reg-sub]]
   [finance-clash-web.components.drawer :refer [panels-label]]
   #_[ocio.components.snackbar-feedback :as snackbar-feedback]))

(reg-sub
 :active-panel
 (fn [db _] (get db :active-panel :default)))

(reg-sub
 :active-panel-label
 :<- [:active-panel]
 (fn [active-panel _]
   (println panels-label)
   (get panels-label active-panel active-panel)))


(reg-sub
 :panel-props-all
 (fn [db _] (get db :panel-props {})))

(reg-sub
 :panel-props
 :<- [:panel-props-all]
 :<- [:active-panel]
 (fn [[m panel-id]] (get m panel-id {})))

(reg-sub
 :loading
 (fn [db [_ request-id]] (get-in db [:loading request-id] false)))

(reg-sub
 :errors
 (fn [db [_ request-id]] (get-in db [:errors request-id] nil)))

(reg-sub
 :user
 (fn [db _] (db :user)))

(reg-sub
 :user-profile
 :<- [:user]
 (fn [user _] (or user {})))

(reg-sub
 :user-logged?
 :<- [:user-profile]
 (fn [user-profile _] (not (empty? user-profile))))

(reg-sub
 :user-role
 :<- [:user-profile]
 (fn [user-profile _]
   (if (:token user-profile)
     (into #{:public :user} (mapv keyword (:roles user-profile)))
     #{:public})))

(reg-sub
 :ui-states
 (fn [db _] (get db :ui-states {})))

(reg-sub
 :drawer-open?
 :<- [:ui-states]
 (fn [m _] (get m :drawer-open? false)))

(reg-sub
 :ui-states-panel
 :<- [:ui-states]
 :<- [:active-panel]
 (fn [[m panel-id]] (get m panel-id {})))

(reg-sub
 :user-input
 (fn [db _] (db :user-input)))

(reg-sub
 :data
 (fn [db _] (db :data)))

(reg-sub
 :help-event
 (fn [db] (get-in db [:help-event])))

;; Simulate the breakpoint from material design

(reg-sub
 :window-size
 (fn [db _]
   (:window-size db)))

(reg-sub
 :window-breakpoint
 :<- [:window-size]
 (fn [{:keys [width height]} _]
   (cond
     (>= width 1920) :xl
     (>= width 1280) :lg
     (>= width 960) :md
     (>= width 600) :sm
     :else :xs)))
