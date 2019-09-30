(ns finance-clash-web.events
  (:require
   [ajax.core :as ajax]
   [day8.re-frame.http-fx]
   [finance-clash-web.db :refer [default-db set-user-ls remove-user-ls]]
   [re-frame.core :as rf :refer
    [reg-event-db reg-event-fx reg-fx inject-cofx trim-v after path debug]]))

#_(goog-define backend-url "http://localhost:3000")
(goog-define backend-url "http://206.81.21.152:3000")

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
   {:db (assoc default-db :user local-store-user)
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
