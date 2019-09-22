(ns finance-clash-web.events
  (:require
   [finance-clash-web.db :refer [default-db set-user-ls remove-user-ls]]
   [re-frame.core :as rf :refer
    [reg-event-db reg-event-fx reg-fx inject-cofx trim-v after path debug]]))

(defn auth-header [db]
  "Get user token and format for API authorization"
  (let [token (get-in db [:user :token])]
    (if token
      [:Authorization (str "Token " token)]
      nil)))

(reg-event-fx
 :initialise-db
 [(inject-cofx :local-store-user)]
 (fn [{:keys [local-store-user]} _]
   ;; take 2 vals from coeffects. Ignore event vector itself.
   {:db (assoc default-db :user local-store-user)
    :dispatch (when (seq local-store-user) [:set-active-panel :welcome])}))

(reg-event-db
 :record-window-size
 (fn [db [_ w h]]
   (assoc db :window-size {:width w :height h})))

(reg-event-db
 :set-active-panel
 (fn [db [_ id]]
   (assoc db :active-panel id)))

(reg-event-db
 :set-panel-props
 (fn [db [_ panel props]]
   (assoc-in db [:panel-props panel] props)))

(reg-event-fx
 :clear-error
 (fn [{:keys [db]} [_ request-type]]
   {:db (assoc-in db [:errors request-type] [])}))
