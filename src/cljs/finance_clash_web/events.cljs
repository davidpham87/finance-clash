(ns finance-clash-web.events
  (:require
   [ajax.core :as ajax]
   [clojure.walk :refer (postwalk-replace postwalk)]

   [datascript.core :as d]
   [day8.re-frame.http-fx]
   [finance-clash-web.components.timer]
   [finance-clash-web.db :refer [default-db empty-ds]]
   [re-frame.core :as rf :refer
    (reg-event-db reg-event-fx reg-fx inject-cofx)]))

(goog-define backend-url "http://localhost:3000")
#_(goog-define backend-url "http://finance-clash-msiai.pro:3000")

(defn user-id [db]
  (get-in db [:user :user/id]))

(defn endpoint [& params]
  (clojure.string/join "/" (concat [backend-url] params)))

(defn auth-header [db]
  "Get user token and format for API authorization"
  (let [token (get-in db [:user :user/token])]
    (if token
      [:Authorization (str "Token " token)]
      nil)))

(reg-event-fx
 :api-request-error
 (fn
   [{:keys [db]} event-vector]
   (let [request-type (second event-vector)
         response (last event-vector)]
     {:db (update-in db [:errors request-type]
                     (fnil conj []) response)})))

(reg-event-db
 ::ds-transact
 (fn [db [_ ds-key tx-data]]
   (update-in db [:ds ds-key] d/db-with tx-data)))

(reg-event-fx
 :clear-error
 (fn [{:keys [db]} [_ request-type]]
   {:db (assoc-in db [:errors request-type] [])}))

(reg-event-fx
 :initialise-db
 [(inject-cofx :local-store-user)]
 (fn [{:keys [local-store-user]} _]
   ;; take 2 vals from coeffects. Ignore event vector itself.
   {:db (assoc default-db :user local-store-user) ;;
    :dispatch-n
    (into [(when (seq local-store-user) [:set-active-panel :welcome])])}))

;; UI

(reg-event-db
 :toggle-drawer
 (fn [db]
   (let [path [:ui-states :drawer-open?]]
     (update-in db path not))))

(reg-event-db
 :close-drawer
 (fn [db]
   (let [path [:ui-states :drawer-open?]]
     (assoc-in db path false))))

(reg-event-db
 :record-window-size
 (fn [db [_ w h]]
   (assoc db :window-size {:width w :height h})))

(reg-event-fx
 :set-active-panel
 (fn [{db :db} [_ id]]
   {:db (assoc db :active-panel id)
    :dispatch [:close-drawer]}))

(reg-event-db
 :set-panel-props
 (fn [db [_ panel props]]
   (assoc-in db [:panel-props panel] props)))

;; Interval event

(defonce interval-handler                ;; notice the use of defonce
  (let [live-intervals (atom {})]        ;; storage for live intervals
    (fn handler [{:keys [action id frequency event]}] ;; the effect handler
      (let [dispatcher #(rf/dispatch event)]
        (case action
          :clean   (doseq [k (keys @live-intervals)]
                     (handler {:action :end :id  k}))
          :start   (when-not (get @live-intervals id)
                     (swap! live-intervals assoc id
                            (js/setInterval dispatcher frequency)))
          :end     (do (js/clearInterval (get @live-intervals id))
                       (swap! live-intervals dissoc id)))))))

;; when this code is reloaded `:clean` existing intervals
(interval-handler {:action :clean})

(reg-fx ;; the re-frame API for registering effect handlers
 :interval ;; the effect id
 interval-handler)

(reg-event-fx
 :clear-intervals
 (fn [_ _]
   {:interval {:action :clean}}))

(reg-event-fx
 :register-interval
 (fn [{db :db} [_ {:keys [id frequency event] :as m}]]
   {:db db
    :interval (assoc m :action :start)}))

(reg-event-fx
 :clear-interval
 (fn [{db :db} [_ {:keys [id frequency event] :as m}]]
   {:db db
    :interval (assoc m :action :end)}))

;; User events
(reg-event-fx
 ::ask-wealth
 (fn [{db :db} _]
   (let [user-id (get-in db [:user :user/id])]
     {:db db
      :http-xhrio {:method :get
                   :headers (auth-header db)
                   :format (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [::success-ask-wealth]
                   :on-failure [:api-request-error]
                   :uri (endpoint "user" user-id "wealth")}})))

(reg-event-fx
 ::success-ask-wealth
 (fn [{db :db} [_ result]]
   {:db (assoc db :wealth (-> result :wealth (js/Math.round)))}))

(reg-event-fx
 ::retrieve-series-question
 (fn [{db :db} _]
   {:db db
    :http-xhrio {:method :get
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [::success-retrieve-series-question]
                 :on-failure [:api-request-error]
                 :uri (endpoint "series" "latest" "questions")}}))

(reg-event-fx
 ::success-retrieve-series-question
 (fn [{db :db} [_ result]]
   (let [m (postwalk-replace {:db/id :datomic.db/id} (first result))]
     (tap> m)
     {:db db
      :fx [[:dispatch
            [::ds-transact :questions
             (into [(-> m (dissoc :problems/questions)
                        (assoc :problems/id "latest"))] (:problems/questions m))]]]})))

;; Quiz events

(reg-event-fx
 ::retrieve-chapters
 (fn [{db :db} _]
   {:db         db
    :http-xhrio {:method          :get
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [::success-retrieve-chapters]
                 :on-failure      [:api-request-error]
                 :uri             (endpoint "quiz" "chapters")}}))

(reg-event-fx
 ::success-retrieve-chapters
 (fn [{db :db} [_ results]]
   (let [data (postwalk-replace {:db/id :datomic.db/id} results)]
     {:db db
      :fx [[:dispatch [::ds-transact :chapters data]]]})))

(reg-event-fx
 ::success-retrieve-questions
 (fn [{db :db} [_ chapter result]]
   (let [result (map-indexed (fn [i m] (assoc m :id (str chapter "_" i))) result)
         result (mapv #(update % :responses
                               (fn [v] (mapv vector (map inc (range)) v)))
                      result)]
     {:db (assoc-in db [:question-data (str chapter)] result)})))

(reg-event-fx
 ::retrieve-answered-questions
 (fn [{db :db} _]
   (let [id (user-id db)]
     {:db db
      :http-xhrio {:method :get
                   :uri (endpoint "user" id "answered-questions")
                   :params {:user-id id}
                   :headers (auth-header db)
                   :format (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [::success-retrieve-answered-questions]
                   :on-failure [:api-request-error]}})))

(reg-event-fx
 ::success-retrieve-answered-questions
 (fn [{db :db} [_ result]]
   (let [data (->> (postwalk-replace {:db/id :datomic.db/id} result)
                   (mapv #(assoc % :question/answered? true)))]
     {:db db
      :fx [[:dispatch [::ds-transact :questions data]]]})))

(reg-event-db
 :set-user-input
 (fn [db [_ k v]]
   (assoc-in db [:user-input k] v)))

(reg-event-fx
 ::commit-change-db
 (fn [{db :db} [_ tx-data]]
   (let [tx-data (mapv #(-> % (assoc :db/id (:datomic.db/id %))
                            (dissoc :datomic.db/id %))
                       tx-data)]
     {:db db
      :http-xhrio {:method :post
                   :uri (endpoint "quiz" "chapters")
                   :params {:tx-data tx-data}
                   :headers (auth-header db)
                   :format (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [::success-retrieve-answered-questions]
                   :on-failure [:api-request-error]}})))

(reg-event-fx
 ::success-commit-change-db
 (fn [{db :db} [_ tx-data]]
   (js/alert "Saved data")
   {:db db}))

(comment
  ::retrieve-questions
  (rf/dispatch [::retrieve-series-question])
  (let [ds (-> @re-frame.db/app-db :ds :questions)]
    #_(map vec (d/datoms ds :eavt))
    (d/q '[:find (pull ?e [*])
           :in $ ?did
           :where [?e :datomic.db/id ?did]]
         ds
         17592186046495))

  (rf/dispatch [::retrieve-chapters])
  )
