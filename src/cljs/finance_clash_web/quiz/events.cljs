(ns finance-clash-web.quiz.events
  (:require
   [ajax.core :as ajax]
   [datascript.core :as d]
   [day8.re-frame.http-fx]
   [finance-clash-web.components.timer :as timer-comp]
   [finance-clash-web.events :as core-events :refer (endpoint auth-header)]
   [re-frame.core :as rf :refer (reg-event-db reg-event-fx)]))

(defn user-id [db]
  (get-in db [:user :user/id]))

(reg-event-db
 ::select-question-phase ;; either :selection or :answering
 (fn [db [_ phase difficulty]]
   (cond-> db
     difficulty (assoc-in [:quiz-question :question/difficulty] difficulty)
     :always (assoc-in [:ui-states :question-phase] phase))))

(reg-event-db
 ::reset-quiz-question
 (fn [db [_ m]]
   (update db :quiz-question merge (or m {}))))

(reg-event-db
 ::reset-quiz-attempts
 (fn [db [_ m]]
   (update db :quiz-question merge {:question/attempts #{}})))

(reg-event-db
 ::set-question-quiz-id
 (fn [db [_ id]]
   (update db :quiz-question assoc :datomic.db/id id)))

(reg-event-db
 ::set-question-quiz-loading
 (fn [db [_ id]]
   (assoc-in db [:quiz-question :status] :loading)))

(reg-event-db
 ::append-question-quiz-attempt
 (fn [db [_ choice]]
   (update-in db [:quiz-question :question/attempts] (fnil conj #{}) choice)))

(reg-event-fx
 ::update-available-questions
 (fn [{db :db} [_ update-type]]
   (let [quiz-question (:quiz-question db)
         path [:series-questions (:difficulty quiz-question)]
         m (get-in db path)]
     (case update-type
       :postpone
       {:db (assoc-in db path (conj (vec (rest m)) (first m)))
        :dispatch [::reset-quiz-question]}
       :answered
       {:db (-> db (update-in [:series-questions-answered] conj (:db/id quiz-question)))
        :dispatch [::reset-quiz-question]}))))

(reg-event-fx
 ::check-question-answer
 (fn [{db :db} [_ question-id user-answer correct?]]
   (let [ds (get-in db [:ds :questions])
         series-id (first (d/q '[:find [?id]
                                 :where
                                 [?s :datomic.db/id ?id]
                                 [?s :problems/id]] ds))
         ;; answers (->> (d/pull ds [:question/answers] [:datomic.db/id question-id])
         ;;              :question/answers
         ;;              (map :answer/position)
         ;;              (into #{}))
         ;; should be many in the schema
         status (if correct? :correct :wrong)]

     (cond->
         {:db (assoc-in db [:quiz-question :status] status)
          :http-xhrio {:method :post
                       :uri (endpoint "quiz" "answer")
                       :headers (auth-header db)
                       :format (ajax/json-request-format)
                       :response-format (ajax/json-response-format {:keywords? true})
                       :params {:user-id (user-id db)
                                :selected-response user-answer
                                :question question-id
                                :series series-id}
                       :on-success [::success-check-question-answer]
                       :on-failure [:api-request-error]}}

       (= :correct status)
       (assoc :dispatch
              [::core-events/ds-transact :questions
               [{:datomic.db/id question-id :question/answered? true}]])))))

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
