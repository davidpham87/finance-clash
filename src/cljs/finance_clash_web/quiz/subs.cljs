(ns finance-clash-web.quiz.subs
  (:require
   [clojure.string :as s]
   [datascript.core :as d]
   [goog.object :as gobj]
   [re-frame.core :as rf :refer (reg-sub)]))

(reg-sub
 ::ds
 (fn [db [_ ds-key]]
   (get-in db [:ds ds-key])))

(reg-sub
 ::question-phase
 (fn [db] (get-in db [:ui-states :question-phase] :selection)))

(reg-sub
 ::questions-answered
 (fn [db]
   (let [ds (get-in db [:ds :questions])]
     (->> (d/q '[:find (pull ?e [:db/id :datomic.db/id])
                 :where
                 [?e :question/answered?]]
               ds)
          (mapv first)))))

(reg-sub
 ::series-questions
 (fn [db]
   (let [series (:series-questions db {:easy [] :medium [] :hard []})
         ds (get-in db [:ds :questions])
         questions
         (->> (d/q '[:find (pull ?e [* {:question/choices [*]}])
                     :in $
                     :where
                     [?e :question/title]
                     (or [(missing? $ ?e :question/answered?)]
                         [?e :question/answered? false])]
                   ds)
              (mapv first)
              (mapv (fn [m] (update m :question/difficulty
                                    #(-> % :db/ident keyword name keyword)))))]
     (group-by :question/difficulty questions))))

(reg-sub
 ::possible
 :<- [::series-questions]
 (fn [m] m))

(reg-sub
 ::questions-shuffled
 :<- [::possible]
 (fn [m] (reduce-kv (fn [m k v] (assoc m k (shuffle v))) {} m)))

(reg-sub
 ::question-selected
 :<- [::possible]
 (fn [m [_ difficulty]] (first (get m difficulty))))

(reg-sub
 ::question
 :<- [::ds :questions]
 (fn [ds [_ datomic-db-id]]
   (d/pull ds '[* {:question/choices [*]}] [:datomic.db/id datomic-db-id])))

(reg-sub
 ::quiz-question
 (fn [db] (:quiz-question db)))

(reg-sub
 ::difficulty
 :<- [::quiz-question]
 (fn [m] (:question/difficulty m)))

(reg-sub
 ::question-status
 :<- [::quiz-question]
 (fn [m] (:status m :loading)))

(reg-sub
 ::previous-attempts
 :<- [::quiz-question]
 (fn [m] (:attempt m {})))

(comment
  @(rf/subscribe [::series-questions])
  @(rf/subscribe [::possible])
  (let [ds (-> @re-frame.db/app-db :ds :questions)]
    (d/q '[:find (pull ?e [*])
           :in $
           :where
           [?e :question/title]
           (or [(missing? $ ?e :question/answered?)]
               [?e :question/answered? false])]
         ds))

  (re-frame.core/clear-subscription-cache!)
  @(rf/subscribe [::questions-answered])
  @(rf/subscribe [::difficulty])
  @(rf/subscribe [::question-selected :hard])
  @(rf/subscribe [::question 17592186046495])
  )
