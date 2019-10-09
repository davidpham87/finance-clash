(ns finance-clash-web.admin.core
  (:require
   ["@material-ui/core" :as mui]
   [finance-clash-web.components.mui-utils :refer
    [cs client-width with-styles text-field input-component panel-style]]
   [goog.object :as gobj]
   [reagent.core :as reagent]
   [finance-clash-web.admin.events :as events]
   [re-frame.core :refer (dispatch subscribe)]))

(defn content []
  (let [username (reagent/atom "")
        password (reagent/atom "")]
    (fn []
      [:> mui/Card {:style {:min-width 480 :width "50vw"}}
       [:> mui/CardHeader {:title "Reset Password"}]
       [:> mui/CardContent
        [:> mui/Grid {:container true :justify :space-between}
         [text-field {:value @username
                      :label "User id"
                      :on-change #(reset! username (.. % -target -value))}]

         [text-field {:value @password
                      :label "New password"
                      :on-change #(reset! password (.. % -target -value))}]]
        [:> mui/Button
         {:color :primary :variant :contained :style {:margin-top 20}
          :on-click #(dispatch [::events/update-password @username @password])}
         "Update password"]]])))

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
