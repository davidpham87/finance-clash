(ns finance-clash-web.admin.edit-question
  (:require
   ["@material-ui/core" :as mui]
   [clojure.set :refer (rename-keys)]
   [datascript.core :as d]
   [finance-clash-web.events]
   [finance-clash-web.admin.events :as events]
   [finance-clash-web.components.mui-utils :refer
    (cs with-styles text-field panel-style select)]
   [finance-clash-web.subs :as subs]
   [goog.object :as gobj]
   [re-frame.core :as rf :refer (dispatch reg-event-fx reg-sub subscribe )]
   [reagent.core :as reagent]))

(reg-sub
 ::ds
 (fn [db [_ ds-key]]
   (get-in db [:ds ds-key])))

(reg-event-fx
 ::stage-modification
 (fn [{db :db} [_ xs]]
   {:db (update db ::staging (fnil into []) xs)}))

(reg-event-fx
 ::reset-staging
 (fn [{db :db}]
   {:db (assoc db ::staging [])}))

(reg-event-fx
 ::commit-change
 (fn [{db :db}]
   (let [tx-data (->> (::staging db [])
                      (group-by :db/id)
                      (reduce-kv (fn [m k v] (assoc m k (apply merge v))) {})
                      vals
                      vec)
         retracts (filter #(= (:kind %) :retract) tx-data)
         tx-data (remove #(= (:kind %) :retract) tx-data)
         tx-data-ds (concat (mapcat :web retracts) tx-data)
         tx-data-db (concat (mapcat :datomic retracts) tx-data)]
     (println (mapcat :datomic retracts))
     {:db db
      :fx [[:dispatch [::reset-staging]]
           [:dispatch [::finance-clash-web.events/ds-transact :chapters tx-data-ds]]
           [:dispatch [::finance-clash-web.events/commit-change-db tx-data-db]]]})))

(reg-sub
 ::chapters-choices
 :<- [::ds :chapters]
 (fn [ds]
   (->>
    (d/q '[:find ?t
           :keys label
           :where [?e :quiz/title ?t]] ds)
    (sort-by :label))))

(reg-sub
 ::chapter-questions
 :<- [::ds :chapters]
 :<- [:user-input-field ::chapter]
 (fn [[ds chapter]]
   (when chapter
     (->> (d/q '[:find (pull ?e [{:quiz/questions [* {:question/choices [*]}]}])
                 :in $ ?c
                 :where
                 [?e :quiz/title ?c]]
               ds chapter)
          ffirst
          :quiz/questions))))

(reg-sub
 ::chapter-question-titles
 :<- [::chapter-questions]
 (fn [xs]
   (->> (mapv #(-> (select-keys % [:question/question :db/id])
                   (rename-keys {:question/question :label :db/id :value})) xs)
        (sort-by :label))))

(reg-sub
 ::chapter-question-choices
 :<- [::ds :chapters]
 :<- [:user-input-field ::question]
 (fn [[ds db-id]]
   (when db-id
     (->> (d/pull ds '[{:question/choices [*]}] db-id)
          :question/choices
          (sort-by :answer/position)))))

(reg-sub
 ::question-data
 :<- [::ds :chapters]
 :<- [:user-input-field ::question]
 (fn [[ds db-id]]
   (when db-id
     (d/pull ds '[:db/id :datomic.db/id {:question/answers [*]}]
             db-id))))

(reg-sub
 ::chapter-question-answer
 :<- [::ds :chapters]
 :<- [:user-input-field ::question]
 (fn [[ds db-id]]
   (when db-id
     (->> (d/pull ds '[:db/id {:question/answers [*]}] db-id)
          :question/answers
          first))))

;; get possible chapters
;; get question from chapters
;; edit question and send it back
(defn select-chapters []
  (let [choices (subscribe [::chapters-choices])]
    (fn []
      (when-not (seq @choices)
        (rf/dispatch [:finance-clash-web.events/retrieve-chapters]))
      [:> mui/FormControl
       {:style {:margin-left 0 :margin-right 20 :min-width 120}}
       [:> mui/InputLabel {:style {:width "100%"}} "Chapters"]
       [select {:choices (for [c @choices] (merge c {:id (:label c)}))
                :on-change
                (fn [event]
                  (rf/dispatch (conj [:set-user-input ::chapter] (.. event -target -value))))
                :subscription-vector [:user-input-field ::chapter]}]])))

(defn select-questions []
  (let [choices (subscribe [::chapter-question-titles])]
    (fn []
      [:> mui/FormControl
       {:style {:margin-left 0 :margin-right 20 :min-width 120
                :width "100%"}}
       [:> mui/InputLabel {:style {:width "100%"}} "Questions"]
       [select {:choices (for [c @choices] (-> (update c :value str)
                                               (assoc :id (:value c))))
                :on-change
                (fn [event]
                  (rf/dispatch (conj [:set-user-input ::question] (.. event -target -value))))
                :subscription-vector [:user-input-field ::question]}]])))

(defn questions-solutions []
  (let [choices (subscribe [::chapter-question-choices])
        question (subscribe [:user-input-field ::question])
        question-choices (subscribe [::chapter-question-titles])
        value (reagent/atom {})
        changed? (reagent/atom #{})]
    (fn []
      (let [show-answers? (contains? (into #{} (map :value @question-choices)) @question)]
        @value
        (when-not show-answers?
          (reset! value {})
          (dispatch [::reset-staging]))
        [:<>
         (for [c @choices
               :let [local-value (reagent/cursor value [(:db/id c)])
                     _ (when (and show-answers? (not @local-value))
                         (reset! local-value (:answer/value c)))
                     ]]
           ^{:key (str (:db/id c))}
           [:div {:style {:margin 20}}
            [text-field
             {:value @local-value
              :fullWidth true
              :multiline true
              :rows 2
              :label (str "Answer No. "(:answer/position c))
              :on-blur #(rf/dispatch [::stage-modification [(assoc c :answer/value @local-value)]])
              :on-change
              #(do (swap! changed? conj (:db/id c))
                   (reset! local-value (.. % -target -value)))}]])]))))

;; assumption for a single correct answer.
(defn correct-answer []
  (let [choices (subscribe [::chapter-question-choices])
        question (subscribe [:user-input-field ::question])
        question-answer (subscribe [::chapter-question-answer])
        question-data (subscribe [::question-data])]
    (fn []

      (when @question
        (rf/dispatch [::reset-staging]))
      (when @question-answer
        (rf/dispatch [:set-user-input ::answer (:answer/position @question-answer)]))
      [:> mui/FormControl
       {:style {:margin-left 0 :margin-right 20 :min-width 120
                :width "100%"}}
       [:> mui/InputLabel {:style {:width "100%"}} "Answer"]
       [select {:default-value (:answer/position @question-answer)
                :choices (vec
                          (for [c @choices]
                            (-> c
                                (assoc :value (:answer/position c))
                                (assoc :id (:answer/position c))
                                (assoc :label (str (:answer/position c))))))
                :on-change
                (fn [event]
                  (let [idx (.. event -target -value)
                        c (first (drop-while #(not= idx (:answer/position %)) @choices))]
                    (rf/dispatch
                     [::stage-modification
                      [{:kind :retract
                        :web
                        (vec
                         (for [x (:question/answers @question-data)]
                           [:db/retract (:db/id @question-data) :question/answers (:db/id x)]))
                        :datomic
                        (vec
                         (for [x (:question/answers @question-data)]
                           [:db/retract (:datomic.db/id @question-data)
                            :question/answers (:datomic.db/id x)]))}
                       (assoc @question-data :question/answers [(select-keys c [:datomic.db/id])])]]))
                  (rf/dispatch (conj [:set-user-input ::answer] (.. event -target -value))))
                :subscription-vector [:user-input-field ::answer]}]])))

(defn submit-button []
  [:> mui/Button {:color :primary
                  :variant :contained
                  :style {:margin-top 20 :margin-bottom 20}
                  :on-click #(rf/dispatch [::commit-change])} "Submit"])

(defn content []
  [:> mui/Grid {:container true :spacing 4 :justify :space-around}
   [:> mui/Card {:style {:min-width "80vw" :margin 10}}
    [:> mui/CardHeader {:title "Reworks questions"}]
    [:> mui/CardContent {:style {:padding 20}}
     [select-chapters]
     [:div {:style {:margin-top 20}} ""]
     [select-questions]
     [questions-solutions]
     [correct-answer]]
    [:> mui/CardActions {:style {:display :flex :width "100%"
                                 :justify-content :flex-end
                                 :padding-right 20}}

     [submit-button]]]])


(defn root [{:keys [classes] :as props}]
  [:main {:class (cs (gobj/get classes "content"))
          :style {:min-height "100vh"
                  :width "100%"
                  :background-position :center
                  :background-color :black
                  :background-size :cover
                  :color :white
                  :z-index 0}}
   [:div {:class (cs (gobj/get classes "appBarSpacer"))}]
   [:div {:style {:min-height 480 :margin-top 5 :height "80%"
                  :display :flex}}
    [:> mui/Fade {:in true :timeout 1000}
     [:div {:style {:margin :auto}}
      [content]]]]])

(comment
  (conj {:a 0} [:b 1])
  (rf/clear-subscription-cache!)
  @(rf/subscribe [::chapters-choices])
  @(rf/subscribe [::chapter-questions])
  @(rf/subscribe [:user-input-field ::chapter])
  @(rf/subscribe [::chapter-question-titles])
  @(rf/subscribe [::chapter-question-choices])
  @(rf/subscribe [::chapter-question-answer])
  (vals (reduce-kv (fn [m k v] (assoc m k (apply merge v)))
                   {}
                   (group-by :a [{:a 3 :b 3} {:a 1 :b 3} {:a 3 :b 10}])))
  )
