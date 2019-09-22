(ns finance-clash-web.quizz.core
  (:require
   [goog.object :as gobj]
   [clojure.string :as s]
   [reagent.core :as reagent]
   [re-frame.core :as rf :refer (dispatch subscribe reg-event-db)]
   [finance-clash-web.components.colors :as colors]
   [finance-clash-web.quizz.subs :as subscriptions]
   [finance-clash-web.components.mui-utils :refer
    [cs client-width custom-theme with-styles text-field input-component panel-style
     adapt-mui-component-style]]
   [finance-clash-web.components.button :refer (submit-button)]
   ["@material-ui/icons/Send" :default ic-send]
   ["@material-ui/core/Grid" :default mui-grid]
   ["@material-ui/core" :as mui]))

;; TODO(dph): insert timer and send answer time by substraction
;; TODO(dph): on timeout send a empty answer

(def answer-button
  (adapt-mui-component-style
   (clj->js {:label {:textTransform "none"}
             :root {:border-radius 3
                    :color "black"
                    :background-color (colors/colors-rgb :aquamarine-bright)
                    "&:hover" {:color "white"
                               :background-color (colors/colors-rgb :aquamarine-dark)}}}) mui/Button))

(defn display-question [{:keys [question responses duration] :as question-map}]
  [:> mui/Card {:style {:max-width 360}}
   [:> mui/CardHeader {:title question}]
   [:> mui/CardContent
    [:div {:display :flex :flex-direction :column :width "100%"
           :justify-content :center :align-items :center}
     (doall
      (for [[i answer] (shuffle responses)]
        ^{:key i}
        [:div {:style {:display :flex :justify-content :center}}
         [:> answer-button
          {:onClick #(js/alert (str "You selected choice answer: " i))
           :variant :outlined
           ;; :disabled true
           :style {:margin-top 10 :margin-bottom 10 :width "100%" :min-height 60}}
          answer]]))]]])

(defn display-question-comp [id]
  (let [data @(subscribe [::subscriptions/question id])]
    (if data
      [display-question data]
      [:div "No Answer"])))

(defn difficulty-button
  ([v] (difficulty-button {} v))
  ([args v]
   [:> mui/Button (merge {:variant :contained :style {:margin 10}} args) v]))

;; TODO(dph): include bonus
(defn difficulty-selection []
  [:> mui/Card {:elevation 0 :style {:min-width 260 :width "50%"}}
   [:> mui/CardHeader {:title "Select Difficulty"}]
   [:> mui/CardContent
    [:div {:style {:display :flex :flex-direction :column}}
     [difficulty-button {:style {:color "white" :margin 10 :background-color
                                 (colors/colors-rgb :aquamarine-dark)}}
      "Easy (-$3/+$10)"]
     [difficulty-button {:style {:margin 10 :background-color
                                 (colors/colors-rgb :citrine-bright)}}
      "Medium (-$7/+$21)"]
     [difficulty-button {:style {:color "white" :margin 10 :background-color
                                 (colors/colors-rgb :red-light-dark)}}
      "Hard (-$10/+$30)"]]]])

#_(defn content [classes]
    [difficulty-selection])

(defn content [classes]
  [display-question-comp "24_3"])

(defn root [m]
  (let []
    (fn [{:keys [classes] :as props}]
      (let []
        [:main {:class (cs (gobj/get classes "content"))
                :style {:background-image "url(images/welcome.jpg)"
                        :background-position :center
                        :color :white
                        :z-index 0}}
         [:div {:class (cs (gobj/get classes "appBarSpacer"))}]
         [:div {:style {:height "80%" :margin-top 60
                        :display :flex :justify-content :center :align-items :center}}
          [:> mui/Fade {:in true :timeout 1000}
           [content classes]]]]))))

(defn root-panel [props]
  [:> (with-styles [panel-style] root) props])
