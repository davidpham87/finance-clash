(ns finance-clash-web.admin.core
  (:require
   ["@material-ui/core" :as mui]
   [finance-clash-web.admin.events :as events]
   [finance-clash-web.admin.edit-question]
   [finance-clash-web.components.mui-utils :refer
    (cs with-styles text-field panel-style)]
   [goog.object :as gobj]
   [re-frame.core :refer (dispatch)]
   [reagent.core :as reagent]))

(defn reset-password []
  (let [username (reagent/atom "")
        password (reagent/atom "")]
    (fn []
      [:<>
       [:> mui/Grid {:container true :justify :space-between :align-items :flex-end :spacing 4}
        [:> mui/Grid {:item true :xs 12 :sm 6}
         [text-field {:value @username
                      :fullWidth true
                      :label "User id"
                      :on-change #(reset! username (.. % -target -value))}]]
        [:> mui/Grid {:item true :xs 12 :sm 6}
         [text-field {:value @password
                      :fullWidth true
                      :label "New password"
                      :on-change #(reset! password (.. % -target -value))}]]]
       [:> mui/Button
        {:color :primary :variant :contained :style {:margin-top 20 :margin-bottom 20}
         :on-click #(dispatch [::events/update-password @username @password])}
        "Update password"]])))

(defn reset-wealth []
  (let [username (reagent/atom "")
        amount (reagent/atom 0)]
    (fn []
      [:<>
       [:> mui/Grid {:container true :justify :space-between :align-items :flex-end :spacing 4}
        [:> mui/Grid {:item true :xs 12 :sm 6}
         [text-field {:value @username
                      :fullWidth true
                      :label "User id"
                      :on-change #(reset! username (.. % -target -value))}]]
        [:> mui/Grid {:item true :xs 12 :sm 6}
         [text-field {:value @amount
                      :type :number
                      :fullWidth true
                      :label "New amount"
                      :on-change #(reset! amount (js/parseInt (.. % -target -value)))}]]]
       [:> mui/Button
        {:color :primary :variant :contained :style {:margin-top 20
                                                     :margin-bottom 20}
         :on-click #(dispatch [::events/update-wealth @username @amount])}
        "Reset wealth"]])))

(defn delete-player []
  (let [username (reagent/atom "")]
    (fn []
      [:<>
       [:> mui/Grid {:container true :justify :space-between
                     :align-items :center :spacing 4}
        [:> mui/Grid {:item true :xs 12 :sm 6}
         [text-field {:value @username
                      :fullWidth true
                      :label "User id"
                      :on-change #(reset! username (.. % -target -value))}]]
        [:> mui/Grid {:item true :xs 12 :sm 6}
         [:> mui/Button
          {:color :primary :variant :contained :style {:margin-top 20
                                                       :margin-bottom 20}
           :on-click #(dispatch [::events/delete-player @username])}
          "Delete player"]]]])))

(defn content []
  [:> mui/Grid {:container true :spacing 4 :justify :space-around}
   (for [[label comp]
         [["Reset Password" reset-password]
          ["Reset Wealth" reset-wealth]
          ["Delete player" delete-player]]]
     ^{:key label}
     [:> mui/Card {:style {:min-width "50vw" :margin 10}}
      [:> mui/CardHeader {:title label}]
      [:> mui/CardContent
       [comp]]])])

(defn root [{:keys [classes] :as props}]
  [:main {:class (cs (gobj/get classes "content"))
          :style {:min-height "100vh"
                  :background-image "url(images/portfolio.jpg)"
                  :background-position :center
                  :background-size :cover
                  :color :white
                  :z-index 0}}
   [:div {:class (cs (gobj/get classes "appBarSpacer"))}]
   [:div {:style {:min-height 480 :margin-top 5 :height "80%"
                  :display :flex :justify-content :center}}
    [:> mui/Fade {:in true :timeout 1000}
     [:div {:style {:margin :auto}}
      [content]]]]])

(defn root-panel [props]
  [:> (with-styles [panel-style] root) props])
