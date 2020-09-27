(ns finance-clash-web.chapter-selection.events
  (:require
   [finance-clash-web.events :refer (endpoint auth-header)]
   [ajax.core :as ajax]
   [day8.re-frame.http-fx]
   [cljs-time.core :as ct]
   [cljs-time.format :as ctf]
   [clojure.walk :refer (postwalk-replace)]
   [re-frame.core :as rf :refer (reg-event-db reg-event-fx dispatch)]))

(reg-event-db
 ::update-available-chapters
 (fn [db [_ id status]]
   (let [f (if (= status :append) conj disj)]
     (update-in db [:chapter-selection :available] (fnil f #{}) id))))

(reg-event-db
 ::update-priority-chapters
 (fn [db [_ id status]]
   (let [f (if (= status :append) conj disj)]
     (update-in db [:chapter-selection :priority] (fnil f #{}) id))))

(reg-event-fx
 ::record-next-series
 (fn [{db :db} [_ quizzes-ids]]
   (let [deadline (ctf/unparse (ctf/formatter "yyyy-MM-dd HH:mm")
                               (ct/plus (ct/today-at 11 0 0 0) (ct/days 1)))]
     {:db db
      :http-xhrio
      {:method :post
       :uri (endpoint "series")
       :headers (auth-header db)
       :format (ajax/json-request-format)
       :response-format (ajax/json-response-format {:keywords? true})
       :params {:deadline deadline
                :quizzes (mapv #(-> {:datomic.db/id %}) quizzes-ids)}
       :on-failure [:api-request-error]
       :on-success [::success-record-next-series]}})))

(reg-event-fx
 ::success-record-next-series
 (fn [{db :db} [_ result]]
   (js/alert "Your settings have been recorded.")
   {:db (update db :series-data (fnil conj []) result)}))

(reg-event-fx
 ::query-chapters
 (fn [{db :db} _]
   {:db db
    :http-xhrio {:method :get
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [::success-query-chapters]
                 :on-failure [:api-request-error]
                 :uri (endpoint "quiz" "chapters")}}))

(reg-event-fx
 ::success-query-chapters
 (fn [{db :db} [_ result]]
   (let [tx-data (->> result
                      (map #(select-keys % [:db/id :quiz/title]))
                      (postwalk-replace {:db/id :datomic.db/id})
                      vec)]
     {:db db
      :fx [[:dispatch [:finance-clash-web.events/ds-transact
                       :questions tx-data]]]})))


(comment
  (rf/dispatch [::query-chapters])
  (ctf/unparse (ctf/formatter "yyyy-MM-dd HH:mm")
               (ct/plus (ct/today-at 9 0 0 0) (ct/days 1)))
  )
