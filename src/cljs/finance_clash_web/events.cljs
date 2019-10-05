(ns finance-clash-web.events
  (:require
   [ajax.core :as ajax]
   [day8.re-frame.http-fx]
   [finance-clash-web.db :refer [default-db set-user-ls remove-user-ls]]
   [finance-clash-web.components.timer]
   [re-frame.core :as rf :refer
    [reg-event-db reg-event-fx reg-fx inject-cofx trim-v after path debug]]))

(goog-define backend-url "http://localhost:3000")
#_(goog-define backend-url "http://finance-clash-msiai.pro:3000")

(defn user-id [db]
  (get-in db [:user :id]))

(defn endpoint [& params]
  (clojure.string/join "/" (concat [backend-url] params)))

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
   {:db default-db ;; (assoc default-db :user local-store-user)
    :dispatch-n
    (into [(when (seq local-store-user) [:set-active-panel :welcome])]
          (mapv #(vector ::retrieve-questions %) (range 26)))}))

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

(reg-event-fx
 :clear-error
 (fn [{:keys [db]} [_ request-type]]
   {:db (assoc-in db [:errors request-type] [])}))

(reg-event-fx
 :api-request-error
 (fn
   [{:keys [db]} event-vector]
   (let [request-type (second event-vector)
         response (last event-vector)]
     {:db (update-in db [:errors request-type]
                     (fnil conj []) response)})))

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


(reg-event-fx
 ::retrieve-latest-series-id
 (fn [{db :db} _]
   {:db db
    :http-xhrio {:method :get
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [::success-retrieve-latest-series-id]
                 :on-failure [:api-request-error]
                 :uri (endpoint "series" "latest")}}))

(reg-event-fx
 ::success-retrieve-latest-series-id
 (fn [{db :db} [_ result]]
   {:db (assoc db :series-id (-> (first result) :id))}))

(reg-event-fx
 ::retrieve-questions
 (fn [{db :db} [_ chapter]]
   (let [question-files (zipmap (range) (:question-files db))
         chapter-file (get question-files chapter)]
     (if chapter-file
       {:db db
        :http-xhrio {:method :get
                     :format (ajax/json-request-format)
                     :response-format (ajax/json-response-format {:keywords? true})
                     :on-success [::success-retrieve-questions chapter]
                     :on-failure [:api-request-error]
                     :uri (str "questions/" chapter-file)}}
       {:db db}))))

(reg-event-fx
 ::success-retrieve-questions
 (fn [{db :db} [_ chapter result]]
   (let [result (map-indexed (fn [i m] (assoc m :id (str chapter "_" i))) result)
         result (mapv #(update % :responses
                               (fn [v] (mapv vector (map inc (range)) v)))
                      result)]
     {:db (assoc-in db [:question-data (str chapter)] result)})))

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
   {:db (assoc db :series-questions result)}))

(reg-event-fx
 ::ask-wealth
 (fn [{db :db} _]
   (let [user-id (get-in db [:user :id])]
     {:db db
      :http-xhrio {:method :get
                   :format (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [::success-ask-wealth]
                   :on-failure [:api-request-error]
                   :uri (endpoint "user" user-id "wealth")}})))

(reg-event-fx
 ::success-ask-wealth
 (fn [{db :db} [_ result]]
   (.log js/console "Finished")
   {:db (assoc db :wealth (-> result first :wealth (js/Math.round)))}))

(reg-event-fx
 ::retrieve-answered-questions
 (fn [{db :db} _]
   (let [id (user-id db)]
     {:db db
      :http-xhrio {:method :get
                   :uri (endpoint "user" id "answered-questions")
                   :params {:user-id id}
                   :format (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [::success-retrieve-answered-questions]
                   :on-failure [:api-request-error]}})))

(reg-event-fx
 ::success-retrieve-answered-questions
 (fn [{db :db} [_ result]]
   {:db (assoc db :questions-answered-data (set (mapv :question result)))}))
