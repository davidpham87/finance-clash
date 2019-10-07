(ns finance-clash-web.quiz.core
  (:require
   ["@material-ui/core" :as mui]
   ["@material-ui/core/Grid" :default mui-grid]
   ["@material-ui/icons/Cancel" :default ic-cancel]
   ["@material-ui/icons/Check" :default ic-check]
   ["@material-ui/icons/Send" :default ic-send]
   ["react-reveal/Bounce" :as reveal-bounce]
   [cljs-time.core :as ct]
   [cljs-time.format :as ctf]
   [clojure.string :as s]
   [finance-clash-web.components.button :refer (submit-button)]
   [finance-clash-web.components.colors :as colors]
   [finance-clash-web.components.mui-utils :refer
    [cs client-width custom-theme with-styles text-field input-component panel-style
     adapt-mui-component-style]]
   [finance-clash-web.components.timer :as timer-comp]
   [finance-clash-web.events :as core-events]
   [finance-clash-web.quiz.events :as events]
   [finance-clash-web.quiz.subs :as subscriptions]
   [goog.object :as gobj]
   [re-frame.core :as rf :refer (dispatch subscribe reg-event-db reg-sub)]
   [reagent.core :as reagent]))

(defn wealth [w]
  [:> mui/Typography {} "Wealth: " w "€"])

(defn wealth-comp []
  (let [w (rf/subscribe [:wealth])]
    (fn [] [wealth @w])))

(def answer-button
  (adapt-mui-component-style
   (clj->js {:label {:textTransform "none"}
             :root {:border-radius 3
                    :color "black"
                    :background-color (colors/colors-rgb :aquamarine-bright)
                    "&:hover" {:color "white"
                               :background-color
                               (colors/colors-rgb :aquamarine-dark)}}})
   mui/Button))

(defn timeout [id]
  (when (zero? @(subscribe [::timer-comp/timer-remaining :quiz]))
    (rf/dispatch [::events/timeout-question id]))
  [:div])

(defn display-question-button [{:keys [answer id i previous-attempts]}]
  (let [timer-remaining @(subscribe [::timer-comp/timer-remaining :quiz])
        outer-comp (if (< (or timer-remaining 12) 11) [:> reveal-bounce] [:<>])]
    (conj outer-comp
          [:div {:style {:display :flex :justify-content :center}}
           [:> answer-button
            {:fullWidth true
             :onClick
             (fn [e]
               (rf/dispatch [::timer-comp/clear-timer :quiz])
               (rf/dispatch [::events/set-question-quiz-loading])
               (rf/dispatch [::events/append-question-quiz-attempt i])
               (rf/dispatch [::events/check-question-answer id i])
               (rf/dispatch [::events/select-question-phase :feedback]))
             :variant :outlined
             :disabled (contains? (or previous-attempts {}) i)
             :style {:margin-top 10 :margin-bottom 10 :width "100%"
                     :min-height 60}}
            answer]])))

(defn timer-comp []
  (let [timer (subscribe [::timer-comp/timer-remaining :quiz])]
    (fn []
      [:div {:style {:color (if (<= @timer 10) :red :grey)}}
       (clojure.core/str "Seconds left: " @timer)])))

(defn display-question
  [{:keys [id question responses duration] :as question-map} previous-attempts]
  (let [module (first (clojure.string/split id "_"))
        module-name @(subscribe [:chapter-name module])]
    [:> mui/Card {:style {:min-width 275 :width "50vw"
                          :background-color "rgba(255, 255, 255, 0.95)"}}
     [:> mui/CardHeader
      {:title question
       :subheader (reagent/as-element
                   [:<>
                    [:div {:style {:display :flex :justifyContent :space-between :margin-top 5}}
                     [wealth-comp] [:div module-name]]
                    [timer-comp]])}]
     [:> mui/CardContent {:style {:height "100%"}}
      [:div {:style {:display :flex :flex-direction :column
                     :width "100%"
                     :height "80%"
                     :flex 1
                     :justify-content :space-around :align-items :stretch}}
       (doall
        (for [[i answer] responses
              :let [args {:answer answer :id id :i i
                          :previous-attempts previous-attempts}]]
          ^{:key i} [display-question-button args]))]]]))

(defn answer-content [status]
  (case status
    :correct
    [:<>
     [:> ic-check {:color :primary :style {:margin-bottom 5 :margin-right 10}}]
     " Right answer!"]
    :wrong
    [:<> [:> ic-cancel {:color :error :style {:margin-bottom 5 :margin-right 10}}]
     " Incorrect answer."]
    :loading
    [:<> [:> mui/CircularProgress {:style {:margin-right 20}}]
     "Checking answer..."]
    [:div "Error, please retry."]))

(defn answer-feedback
  [{:keys [status] :or {status :correct}}]
  [:> mui/Dialog {:open true}
   [:> mui/DialogTitle
    [:<> "Question Feedback"
     [:div {:style {:opacity 0.7}} [wealth-comp]]]]
   [:> mui/DialogContent {:style {:margin :auto :width "40vh"}}
    [:> mui/DialogContentText
     {:style {:display :flex ;; :flex-direction :column
              :align-items :center :justify :center}}
     [answer-content status]]]
   [:> mui/DialogActions
    (when (= status :wrong)
      [:> mui/Button {:onClick #(dispatch [::events/select-question-phase
                                           :answering])} "Retry"])
    [:> mui/Button
     {:onClick (fn []
                 (dispatch [::events/update-available-questions
                            (if (= status :correct) :answered :postpone)])
                 (dispatch [::events/select-question-phase :selection]))}
     "Next"]]])

(defn ->isoformat [d]
  (ctf/unparse (ctf/formatters :mysql) d))

(defn display-question-comp [difficulty]
  (let [question-selected @(subscribe [::subscriptions/question-selected difficulty])
        data @(subscribe [::subscriptions/question question-selected])
        duration (:duration data 10)
        previous-attempts @(subscribe [::subscriptions/previous-attempts])]
    (when (seq question-selected)
      (rf/dispatch [::events/set-question-quiz-id question-selected]))
    (if data
      (do
        (rf/dispatch [::events/pay-question difficulty])
        (rf/dispatch [::timer-comp/start-timer
                      {:id :quiz :duration duration
                       :start-time (->isoformat (ct/now))
                       :end-time (-> (ct/now)
                                     (ct/plus (ct/seconds duration))
                                     ->isoformat)
                       :remaining (+ 0 duration)}])
        [:<>
         [timeout (:id data)]
         [display-question (update data :responses shuffle) previous-attempts]])
      [:div "No data"])))

(defn difficulty-button
  ([v] (difficulty-button {} v))
  ([args v]
   [:> mui/Button (merge {:fullWidth true :variant :contained} args) v]))

(defn difficulty-buttons [questions-available]
  (let [event [::events/select-question-phase :answering]
        questions-count (fn [k] (count (get questions-available k [])))]
    [:> mui/Grid {:container true :style {:flex-grow 1 :height "80%"}
                      :alignItems :center}
         [:> mui/Grid {:item true :xs 12}
          [:> mui/Grid {:container true :justify :space-around
                        :direction :column :alignItems :stretch
                        :style {:height 300}}
           [:> mui/Grid {:item true}
            [difficulty-button
             {:on-click #(dispatch (conj event :easy))
              :disabled (zero? (questions-count :easy))
              :style {:color "white":background-color
                      (colors/colors-rgb ;; :aquamarine-dark
                       :nucleus-blue-dark)}}
             (str "Easy (-€3 / +€12) " "[" (questions-count :easy)  "]")]]
           [:> mui/Grid {:item true}
            [difficulty-button
             {:on-click #(dispatch (conj event :medium))
              :disabled (zero? (questions-count :medium))
              :style {:background-color (colors/colors-rgb :citrine-bright)}}
             (str "Medium (-€7 / +€28) " "[" (questions-count :medium) "]")]]
           [:> mui/Grid {:item true}
            [difficulty-button
             {:on-click #(dispatch (conj event :hard))
              :disabled (zero? (questions-count :hard))
              :style {:color "white" :background-color
                      (colors/colors-rgb :red-light-dark)}}
             (str "Hard (-€10 / +€40) " "[" (questions-count :hard) "]")]]]]]))

(defn difficulty-selection [questions-available]
  (let [questions-remaining? (pos? (reduce + (mapv count (vals questions-available))))]
    [:> mui/Card {:elevation 0 :style
                  {:margin :auto
                   :background-color "rgba(255, 255, 255, 0.95)"
                   :min-width "50vw"
                   :max-height 420 :height "80%"}}
     [:> mui/CardHeader {:title "Select Difficulty"
                         :subheader (reagent/as-element [wealth-comp])}]
     [:> mui/CardContent {:style {:height "100%" :min-width 260 :width "100%"}}
      [:> mui/Typography
       (if questions-remaining?
         "Rewards without bonus/malus."
         [:div {:style {:color (colors/colors-rgb :red-light)}}
          "No more available question."])]
      (when questions-remaining?
        [difficulty-buttons questions-available])]]))

(defmulti content :phase :default :selection)

(defmethod content :selection [m]
  (rf/dispatch [::core-events/ask-wealth])
  [difficulty-selection @(subscribe [::subscriptions/series-questions])])

(defmethod content :answering [m]
  (rf/dispatch [::core-events/ask-wealth])
  [:<>
   [display-question-comp @(subscribe [::subscriptions/difficulty])]])

(defmethod content :feedback [m]
  [answer-feedback {:status @(subscribe [::subscriptions/question-status])}])

(defn init-events []
  (rf/dispatch [::finance-clash-web.events/retrieve-series-question])
  (rf/dispatch [::events/query-latest-series])
  (rf/dispatch [::core-events/retrieve-answered-questions])
  #_(rf/dispatch [::events/select-question-phase :selection]))

(defn root [m]
  (let [question-phase (subscribe [::subscriptions/question-phase])]
    (init-events)
    (fn [{:keys [classes] :as props}]
      (let []
        [:main {:class (cs (gobj/get classes "content"))
                :style {:background-image "url(images/daily_questions.jpg)"
                        :background-position :center
                        :background-size :cover
                        :color :white
                        :z-index 0}}
         [:div {:class (cs (gobj/get classes "appBarSpacer"))}]
         [:div {:style {:min-height 480 :margin-top 5 :height "80%"
                        :display :flex :justify-content :center}}
          [:> mui/Fade {:in true :timeout 1000}
           [:div {:style {:margin :auto}}
            [content {:phase @question-phase}]]]]]))))

(defn root-panel [props]
  [:> (with-styles [panel-style] root) props])
