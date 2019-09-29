(ns finance-clash-web.quizz.core
  (:require
   [goog.object :as gobj]
   [clojure.string :as s]
   [reagent.core :as reagent]
   [re-frame.core :as rf :refer (dispatch subscribe reg-event-db reg-sub)]
   [finance-clash-web.components.colors :as colors]
   [finance-clash-web.quizz.subs :as subscriptions]
   [finance-clash-web.components.mui-utils :refer
    [cs client-width custom-theme with-styles text-field input-component panel-style
     adapt-mui-component-style]]
   [finance-clash-web.components.button :refer (submit-button)]
   ["@material-ui/icons/Send" :default ic-send]
   ["@material-ui/icons/Check" :default ic-check]
   ["@material-ui/icons/Cancel" :default ic-cancel]

   ["@material-ui/core/Grid" :default mui-grid]
   ["@material-ui/core" :as mui]))


;; TODO(dph):
;; Implement: check answer with backend. On successful answer remove the
;; question from possible answer. When giving up, put the question at the back
;; the list.

;; TODO(dph): insert timer and send answer time by substraction
;; TODO(dph): on timeout send a empty answer
(reg-event-db
 ::select-question-phase ;; either :selection or :answering
 (fn [db [_ phase difficulty]]
   (cond-> db
     difficulty (assoc-in [:quizz-question :difficulty] difficulty)
     :always(assoc-in [:ui-states :question-phase] phase))))

(def answer-button
  (adapt-mui-component-style
   (clj->js {:label {:textTransform "none"}
             :root {:border-radius 3
                    :color "black"
                    :background-color (colors/colors-rgb :aquamarine-bright)
                    "&:hover" {:color "white"
                               :background-color (colors/colors-rgb :aquamarine-dark)}}})
   mui/Button))

(defn display-question [{:keys [id question responses duration] :as question-map}]
  (let [module (first (clojure.string/split id "_"))]
    [:> mui/Card {:style {:max-width 360}}
     [:> mui/CardHeader {:title (str "[" module  "] "question)}]
     [:> mui/CardContent {:style {:height "100%"}}
      [:div {:style {:display :flex :flex-direction :column
                     :width "100%"
                     :height "80%"
                     :flex 1
                     :justify-content :space-around :align-items :stretch}}
       (doall
        (for [[i answer] (shuffle responses)]
          ^{:key i}
          [:div {:style {:display :flex :justify-content :center}}
           [:> answer-button
            {:fullWidth true
             :onClick
             (fn [e]
               #_(js/alert (str "You selected choice answer: " i))
               (rf/dispatch [::select-question-phase :feedback]))
             :variant :outlined
             ;; :disabled true
             :style {:margin-top 10 :margin-bottom 10 :width "100%" :min-height 60}}
            answer]]))]]]))

(defn answer-content [status]

  (case status
    :loading
    [:<> [:> mui/CircularProgress {:style {:margin-right 20}}]
     "Checking answer..."]

    :correct
    [:<>
     [:> ic-check {:color :primary :style {:margin-bottom 5 :margin-right 10}}]
     " Right answer!"]

    :wrong
    [:<> [:> ic-cancel {:color :error :style {:margin-bottom 5 :margin-right 10}}]
     " Incorrect answer."]))

(defn answer-feedback
  [{:keys [status] :or {status :correct}}]
  [:> mui/Dialog {:open true}
   [:> mui/DialogTitle "Question Feedback"]
   [:> mui/DialogContent
    [:> mui/DialogContentText {:style {:display :flex :align-items :center
                                       :justify :center}}
     [answer-content status]]]
   [:> mui/DialogActions
    (when (= status :wrong)
      [:> mui/Button {:onClick #(dispatch [::select-question-phase :answering])}
       "Retry"])
    [:> mui/Button {:onClick #(dispatch [::select-question-phase :selection])}
     "Next"]]])

(defn display-question-comp [difficulty]
  (let [question-selected @(subscribe [::subscriptions/question-selected difficulty])
        data @(subscribe [::subscriptions/question question-selected])]
    (if data
      [display-question data]
      [:div "No data"])))

(defn difficulty-button
  ([v] (difficulty-button {} v))
  ([args v]
   [:> mui/Button (merge {:fullWidth true :variant :contained} args) v]))

;; TODO(dph): include bonus
(defn difficulty-selection []
  (let [event [::select-question-phase :answering]]
    [:> mui/Card {:elevation 0 :style
                  {:margin :auto
                   :min-width 260 :width "50%"
                   :max-height 400 :height "80%"}}
     [:> mui/CardHeader {:title "Select Difficulty"}]
     [:> mui/CardContent {:style {:height "100%"}}
      [:> mui/Grid {:container true :style {:flex-grow 1 :height "80%"}
                    :alignItems :center}
       [:> mui/Grid {:item true :xs 12}
        [:> mui/Grid {:container true :justify :space-around
                      :direction :column :alignItems :stretch
                      :style {:height 300}}
         [:> mui/Grid {:item true}
          [difficulty-button
           {:on-click #(dispatch (conj event :easy))
            :style {:color "white":background-color
                    (colors/colors-rgb ;; :aquamarine-dark
                     :nucleus-blue-dark
                     )}}
           "Easy (-$3 / +$10)"]]
         [:> mui/Grid {:item true}
          [difficulty-button
           {:on-click #(dispatch (conj event :medium))
            :style {:background-color (colors/colors-rgb :citrine-bright)}}
           "Medium (-$7 / +$21)"]]
         [:> mui/Grid {:item true}
          [difficulty-button
           {:on-click #(dispatch (conj event :hard))
            :style {:color "white" :background-color
                    (colors/colors-rgb :red-light-dark)}}
           "Hard (-$10 / +$30)"]]]]]]]))

(defmulti content :phase :default :selection)

(defmethod content :selection [m]
  [difficulty-selection])

(defmethod content :answering [m]
  [display-question-comp @(subscribe [::subscriptions/difficulty])])

(defmethod content :feedback [m]
  [answer-feedback {:status :correct}])

(defn root [m]
  (let [question-phase (subscribe [::subscriptions/question-phase])]
    #_(dispatch [::select-question-phase :selection])
    (fn [{:keys [classes] :as props}]
      (let []
        [:main {:class (cs (gobj/get classes "content"))
                :style {:background-image "url(images/welcome.jpg)"
                        :background-position :center
                        :color :white
                        :z-index 0}}
         [:div {:class (cs (gobj/get classes "appBarSpacer"))}]
         [:div {:style {:min-height 480 :margin-top 5
                        :display :flex :justify-content :center}}
          [content {:phase @question-phase}]
          #_[:> mui/Fade {:in true :timeout 1000}
           [content {:phase @question-phase}]]]]))))

(defn root-panel [props]
  [:> (with-styles [panel-style] root) props])
