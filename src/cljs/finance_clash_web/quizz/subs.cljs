(ns finance-clash-web.quizz.subs
  (:require
   [goog.object :as gobj]
   [clojure.string :as s]
   [re-frame.core :as rf :refer (reg-sub)]))

(reg-sub
 ::question-phase
 (fn [db _]
   (get-in db [:ui-states :question-phase] :selection)))

(reg-sub
 ::series-questions
 (fn [db]
   (:series-questions db {:easy [] :medium [] :hard []})))

(reg-sub
 ::possible
 :<- [::series-questions]
 (fn [m _]
   (reduce-kv (fn [m k v] (assoc m k (first v))) {} m)))

(reg-sub
 ::question-selected
 :<- [::possible]
 (fn [m [_ difficulty]]
   (get m difficulty)))

(reg-sub
 ::question
 (fn [db [_ id]]
   (let [[chapter question] (s/split id #"_")
         question (js/parseInt question)]
     (get-in db [:question-data chapter question]))))

(reg-sub
 ::difficulty
 (fn [db _]
   (get-in db [:quizz-question :difficulty])))
