(ns finance-clash-web.chapter-selection.events
  (:require
   [finance-clash-web.events :refer (endpoint)]
   [ajax.core :as ajax]
   [day8.re-frame.http-fx]
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
 (fn [{db :db} [_ available-ids priority-ids]]
   {:db db
    :http-xhrio
    {:method :post
     :uri (endpoint "series")
     :format (ajax/json-request-format)
     :response-format (ajax/json-response-format {:keywords? true})
     :params {:available (sort (mapv js/parseInt available-ids))
              :priority (sort (mapv js/parseInt priority-ids))}
     :on-failure [:api-request-error]
     :on-success [::success-record-next-series]}}))

(reg-event-fx
 ::success-record-next-series
 (fn [{db :db} [_ result]]
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
   (let [available-ids (->> (filter #(pos? (:available %)) result)
                            (map #(-> % :chapter str)) set)
         priority-ids (->> (filter #(pos? (:priority %)) result)
                           (map #(-> % :chapter str)) set)]
     {:db (assoc db :chapter-selection {:available available-ids
                                        :priority priority-ids})})))
;; TODO(dph): request series and store it in series
