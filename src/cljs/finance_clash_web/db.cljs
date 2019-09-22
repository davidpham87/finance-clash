(ns finance-clash-web.db
  (:require
   [re-frame.core :as rf :refer (reg-cofx)]))

(def default-db
  {:active-panel :login
   :panel-props {} ;; hack to dispatch arguments to component in lazy mode
   :credentials {}
   :user {} ;; data for auth
   :user-input {}
   :loading {}
   :errors {}
   :help-event [:set-panel :welcome]
   :ui-states
   {:drawer-open? false
    :drawer-displayed-sublists #{}}})

(def fcw-user-key "finance-clash-web-user")  ;; localstore key

(defn set-user-ls
  "Puts user into localStorage"
  [user]
  (.setItem js/localStorage fcw-user-key (str user))) ;; sorted-map written as an EDN map

;; Removes user information from localStorge when a user logs out.
(defn remove-user-ls
  "Removes user from localStorage"
  []
  (.removeItem js/localStorage fcw-user-key))

;; -- cofx Registrations  -----------------------------------------------------
;;
;; To see it used, look in `events.cljs` at the event handler for `:initialise-db`.
;; That event handler has the interceptor `(inject-cofx :local-store-user)`
;; The function registered below will be used to fulfill that request.
;;
;; We must supply a `sorted-map` but in localStorage it is stored as a `map`.
(reg-cofx
 :local-store-user
 (fn [cofx _]
   (assoc cofx :local-store-user
          (into (sorted-map)
                (some->> (.getItem js/localStorage fcw-user-key)
                         (cljs.reader/read-string))))))
