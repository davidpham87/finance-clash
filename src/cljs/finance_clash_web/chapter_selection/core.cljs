(ns finance-clash-web.chapter-selection.core
  (:require
   ["@material-ui/core" :as mui]
   ["@material-ui/core/Grid" :default mui-grid]
   ["@material-ui/core/List" :default mui-list]
   ["@material-ui/core/ListItem" :default mui-list-item]
   ["@material-ui/core/ListItemText" :default mui-list-item-text]
   ["@material-ui/icons/Send" :default ic-send]
   [clojure.string :as s]
   [finance-clash-web.chapter-selection.events :as events]
   [finance-clash-web.chapter-selection.subs :as subscriptions]
   [finance-clash-web.components.button :refer (submit-button)]
   [finance-clash-web.components.mui-utils :refer (cs with-styles panel-style)]
   [goog.object :as gobj]
   [re-frame.core :as rf :refer (dispatch subscribe)]
   [reagent.ratom :refer-macros (reaction)]))

(def question-files
  ["0_Key_Notions.yaml"
   "1_Intro.yaml"
   "2_Etats_Financiers.yaml"
   "3_Le_Bilan.yaml"
   "4_Le_Compte_de_Resultat.yaml"
   "5_Introduction_Finance.yaml"
   "6_Le_Capital.yaml"
   "7_Cycles_Exploitation_Inv_Financement.yaml"
   "8_Comptabilite.yaml"
   "9_SIG_et_CAF.yaml"
   "10_Obligations.yaml"
   "11_Regulatory_Requirements.yaml"
   "12_Financial_Crisis.yaml"
   "13_TVA.yaml"
   "14_Diagnostic_Financier.yaml"
   "15_Financial_Risks_and_Options.yaml"
   "16_Couts_and_Comptabilite_Analytique.yaml"
   "17_Microeconomie.yaml"
   "18_Central_Banks.yaml"
   "19_Tresorerie.yaml"
   "20_empty.yaml"
   "21_Rating_agencies.yaml"
   "22_Credit_Talk.yaml"
   "23_Libor_Fwd_Rates.yaml"
   "24_Bourse.yaml"
   "25_Liquidity_Talk.yaml"])

(defn ->question-data
  [question]
  (let [x (-> question
              (s/split #"\.")
              first
              (s/split #"_" 2))
        m (update (zipmap [:chapter :title] x)
                  :title #(clojure.string/replace % "_" " "))] m))

(def question-data (mapv ->question-data question-files))

(defn send-button []
  (let [available-ids @(subscribe [::subscriptions/chapter-available])
        priority-ids @(subscribe [::subscriptions/chapter-priority])
        super-user? @(subscribe [:super-user?])]
    (if super-user?
      [submit-button [::events/record-next-series available-ids priority-ids]]
      [:div])))

(defn center [& children]
  [:div {:style {:display :flex :justify-content :center}}
   children])

;; TODO (Create more fined grined subscription events
;; for avoiding rerendering all of the checkbox)
(defn chapters [ms checked-chapters]
  (let [super-user? @(subscribe [:super-user?])]
    [:> mui-list
     (for [m ms]
       ^{:key (:db/id m)}
       [:> mui-list-item
        {:dense true
         :style {:min-width "40vw"
                 :display :flex
                 :justify-content :space-between
                 :align-items :center}}
        [:> mui-list-item-text (:quiz/title m)]
        [:<>
         [:> mui/Tooltip {:title "Available" :placement :left :enterDelay 500}
          [:> mui/Checkbox
           {:checked (contains? (:available checked-chapters) (:datomic.db/id m))
            :disabled (not super-user?)
            :onChange
            (fn [e]
              (let [status (if (.. e -target -checked) :append :remove)]
                (rf/dispatch [::events/update-available-chapters
                              (:datomic.db/id m) status])))}]]]])]))

(defn chapters-comp [ms]
  (let [checked-chapters
        (reaction {:available @(subscribe [::subscriptions/chapter-available])
                   :priority @(subscribe [::subscriptions/chapter-priority])})]
    (fn [ms]
      [chapters ms @checked-chapters])))

(defn content [_]
  (let [question-data (subscribe [::subscriptions/chapters])]
    (fn []
      [:<>
       [:> mui/Grid {:item true :xs 12}
        [:> mui/Card {:elevation 0}
         [:> mui/CardHeader {:title "Module Selection"}]]]
       [chapters-comp @question-data]
       [:> mui-grid {:item true :xs 12 :style {:margin-bottom 10}}
        [center [send-button]]]])))

(defn root [m]
  (dispatch [::events/query-chapters])
  (fn [{:keys [classes]}]
    [:main {:class (cs (gobj/get classes "content"))
            :style {:background-image "url(images/chapter.jpg)"
                    :background-position :center
                    :background-size :cover
                    :color :white
                    :z-index 0}}
     [:div {:class (cs (gobj/get classes "appBarSpacer"))}]
     [:> mui/Fade {:in true :timeout 1000}
      [:> mui/Grid {:container true :justify "center"}
       [:> mui/Paper
        {:elevation 1
         :style {:margin 5
                 :padding 5
                 :background-position :center
                 :background-color "rgba(255,255,255,0.8)"
                 :color "black" :width "100%"
                 :z-index 10}}
        [:> mui/Grid {:container true :justify "center" :alignItems :flex-end}
         [content classes]]]]]]))

(defn root-panel [props]
  [:> (with-styles [panel-style] root) props])
