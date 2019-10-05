(ns finance-clash-web.quiz.subs
  (:require
   [goog.object :as gobj]
   [clojure.string :as s]
   [re-frame.core :as rf :refer (reg-sub)]))

(reg-sub
 ::question-phase
 (fn [db] (get-in db [:ui-states :question-phase] :selection)))

(reg-sub
 ::questions-answered
 (fn [{db :db}]
   (:questions-answered-data db #{})))

(reg-sub
 ::series-questions
 (fn [db]
   (let [series (:series-questions db {:easy [] :medium [] :hard []})
         questions-answered (:questions-answered-data db #{})]
     (reduce-kv (fn [m k v] (assoc m k (remove questions-answered v))) {} series))))

(reg-sub
 ::possible
 :<- [::series-questions]
 (fn [m] (reduce-kv (fn [m k v] (assoc m k (first v))) {} m)))

(reg-sub
 ::question-selected
 :<- [::possible]
 (fn [m [_ difficulty]] (get m difficulty)))

(reg-sub
 ::question
 (fn [db [_ id]]
   (let [[chapter question] (s/split id #"_")
         question (js/parseInt question)]
     (get-in db [:question-data chapter question]))))

(reg-sub
 ::quiz-question
 (fn [db] (:quiz-question db)))

(reg-sub
 ::difficulty
 :<- [::quiz-question]
 (fn [m] (:difficulty m)))

(reg-sub
 ::question-status
 :<- [::quiz-question]
 (fn [m] (:status m :loading)))

(reg-sub
 ::previous-attempts
 :<- [::quiz-question]
 (fn [m] (:attempt m {})))
