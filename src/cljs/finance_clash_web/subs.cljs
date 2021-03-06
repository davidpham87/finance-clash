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
 :<- [:user]
 (fn [user] (not (empty? user))))

(reg-sub
 :user-role
 :<- [:user-profile]
 (fn [user-profile _]
   (if (:user/token user-profile)
     (into #{:public :user} (mapv keyword (:user/roles user-profile)))
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
 :user-input-field
 :<- [:user-input]
 (fn [m [_ k]] (get m k)))

(reg-sub
 :data
 (fn [db _] (db :data)))

(reg-sub
 :help-event
 (fn [db] (get-in db [:help-event])))

(reg-sub
 :wealth
 (fn [db _] (:wealth db)))

(reg-sub
 :chapter-name
 (fn [db [_ id]] (get-in db [:chapter-names id])))
;; Simulate the breakpoint from material design

(reg-sub
 :super-user?
 (fn [db _]
   (let [id (get-in db [:user :user/id])
         super-users (:super-users db)]
     (contains? super-users id))))

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

(comment
  @(rf/subscribe [:super-user?])
  (contains? (get-in @re-frame.db/app-db [:super-users]) (get-in @re-frame.db/app-db [:user :user/id]))
  )
