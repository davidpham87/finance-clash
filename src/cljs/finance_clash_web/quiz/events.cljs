(ns finance-clash-web.quiz.events
  (:require
   [finance-clash-web.components.timer :as timer-comp]
   [finance-clash-web.events :as core-events :refer (endpoint auth-header)]
   [ajax.core :as ajax]
   [day8.re-frame.http-fx]
   [re-frame.core :as rf :refer (reg-event-db reg-event-fx)]))

(defn user-id [db]
  (get-in db [:user :id]))

(reg-event-db
 ::select-question-phase ;; either :selection or :answering
 (fn [db [_ phase difficulty]]
   (cond-> db
     difficulty (assoc-in [:quiz-question :difficulty] difficulty)
     :always(assoc-in [:ui-states :question-phase] phase))))

(reg-event-db
 ::reset-quiz-question
 (fn [db _]
   (assoc db :quiz-question {})))

(reg-event-db
 ::set-question-quiz-id
 (fn [db [_ id]]
   (update db :quiz-question assoc :id id)))

(reg-event-db
 ::set-question-quiz-loading
 (fn [db [_ id]]
   (assoc-in db [:quiz-question :status] :loading)))

(reg-event-db
 ::append-question-quiz-attempt
 (fn [db [_ choice]]
   (update-in db [:quiz-question :attempt] (fnil conj #{}) choice)))

(reg-event-fx
 ::update-available-questions
 (fn [{db :db} [_ update-type]]
   (let [quiz-question (:quiz-question db)
         path [:series-questions (:difficulty quiz-question)]
         question-id (:id quiz-question)
         m (get-in db path)]
     (case update-type
       :postpone
       {:db (assoc-in db path (conj (vec (rest m)) (first m)))
        :dispatch [::reset-quiz-question]}
       :answered
       {:db (-> db
                (assoc-in path (vec (remove #{(:id quiz-question)} (rest m))))
                (update-in [:series-question-answered] conj (:id quiz-question)))
        :dispatch [::reset-quiz-question]}))))

(reg-event-fx
 ::query-latest-series
 (fn [{db :db} _]
   {:db db
    :http-xhrio
    {:method :get
     :uri (endpoint "series" "latest")
     :format (ajax/json-request-format)
     :response-format (ajax/json-response-format {:keywords? true})
     :on-success [::success-query-latest-series]
     :on-failure [:api-request-error]}}))

(reg-event-fx
 ::success-query-latest-series
 (fn [{db :db} [_ result]]
   {:db (assoc db :series-data (-> result :series first))}))

(reg-event-fx
 ::check-question-answer
 (fn [{db :db} [_ question-id user-answer]]
   (let [[chapter question] (clojure.string/split question-id #"_")]
     {:db db
      :http-xhrio {:method :post
                   :uri (endpoint "quiz" chapter question "answer")
                   :format (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :params {:user-id (user-id db) :selected-response user-answer
                            :series (get-in db [:series-data :id])}
                   :on-success [::success-check-question-answer]
                   :on-failure [:api-request-error]}})))

(reg-event-fx
 ::success-check-question-answer
 (fn [{db :db} [_ result]]
   {:db (assoc-in db [:quiz-question :status]
                  (keyword (:answer-status result :loading)))
    :dispatch [::core-events/ask-wealth]}))

(reg-event-fx
 ::pay-question
 (fn [{db :db} [_ difficulty]]
   {:db db
    :http-xhrio {:method :post
                 :uri (endpoint "quiz" "buy-question")
                 :headers (auth-header db)
                 :params {:user-id (user-id db) :difficulty difficulty}
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [::success-pay-question]
                 :on-failure [:api-request-error]}}))

(reg-event-fx
 ::success-pay-question
 (fn [{db :db} [_ result]]
   {:db (assoc db :wealth (- (:wealth db) (:cost result)))}))

(reg-event-fx
 ::timeout-question
 (fn [{db :db} [_ id]]
   {:dispatch-n [[::timer-comp/clear-timer :quiz]
                 [::set-question-quiz-loading]
                 [::check-question-answer id 0]
                 [::select-question-phase :feedback]]}))
