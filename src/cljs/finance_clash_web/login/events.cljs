(ns finance-clash-web.login.events
  (:require
   [ajax.core :as ajax]
   [clojure.set :refer (rename-keys)]
   [day8.re-frame.http-fx]
   [finance-clash-web.events :refer (endpoint auth-header)]
   [finance-clash-web.db :refer (default-db set-user-ls remove-user-ls)]
   [re-frame.core :as rf :refer
    (reg-event-db reg-event-fx reg-fx inject-cofx trim-v after path debug)]))

;; Login
(def set-user-interceptor
  [(path :user)        ;; `:user` path within `db`, rather than the full `db`.
   (after set-user-ls) ;; write user to localstore (after)
   trim-v])            ;; removes first (event id) element from the event vec

(def remove-user-interceptor [(after remove-user-ls)])

(reg-event-fx
 ::login
 (fn [{db :db} [_ credentials]]
   {:db db
    :http-xhrio
    {:method :put
     :uri (endpoint "user")
     :params (rename-keys (select-keys credentials [:email :password])
                          {:email :id})
     :format (ajax/json-request-format)
     :response-format (ajax/json-response-format {:keywords? true})
     :on-success [::success-login]
     :on-failure [:api-request-error ::login]}}))

(reg-event-fx
 ::success-login
 set-user-interceptor
 (fn [{user :db} [{result :user}]]
   (println result)
   {:db (merge user result)
    :dispatch [:set-active-panel :quiz]}))

(reg-event-fx ;; usage (dispatch [:user/register registration])
 ::register ;; triggered when a users submits registration form
 (fn
   [{:keys [db]} [_ registration]]
  {:db db
   :http-xhrio
   {:method :post
    :uri (endpoint "user") ;; evaluates to "api/user"
    :params (rename-keys (select-keys registration [:email :password])
                         {:email :id}) ;; {:id ... :password ...}
    :format (ajax/json-request-format)
    :response-format (ajax/json-response-format {:keywords? true})
    :on-success [::success-register]
    :on-failure [:api-request-error ::register]}}))

(reg-event-fx
 ::success-register
 set-user-interceptor
 (fn [{user :db} [m]]
   (let [props (:user m)]
     {:db (merge user props)
      :dispatch  [:set-active-panel :quiz]})))

(reg-event-fx
 ::logout
 remove-user-interceptor
 (fn [{:keys [db]} _]
   {:db (assoc db :user {}) ;; reset users
    :dispatch-n [[:initialise-db]
                 [:set-active-panel :login]]}))

(reg-event-fx
 ::register-validation-error
 (fn
   [{:keys [db]} [_ error-kind message]]
   {:db (update-in db [:errors ::register]
                   (fnil conj []) {:type error-kind :message message})}))

(reg-event-fx
 ::update-userprofile
 (fn
   [{:keys [db]} [_ user]] ;; user = {:img ... :username ... :bio ... :email ... :password ...}
   {:db db
    :http-xhrio {:method :put
                 :uri (endpoint "user" (:id user))
                 :params (-> user (select-keys [:username :id :password]))
                 :headers (auth-header db)
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [::success-update-userprofile]
                 :on-failure [:api-request-error ::update-userprofile]}}))

(reg-event-fx
 ::success-update-userprofile
 (fn [{db :db} [_ result]]
   {:db (update db :user merge result)
    :dispatch [:clear-error ::register]}))
