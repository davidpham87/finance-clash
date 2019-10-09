(ns finance-clash-web.ranking.core
  (:require
   ["@material-ui/core" :as mui]
   [finance-clash-web.components.mui-utils :refer
    [cs client-width with-styles text-field input-component panel-style]]
   [goog.object :as gobj]
   [reagent.core :as reagent]
   [finance-clash-web.ranking.events :as events]
   [finance-clash-web.ranking.subs :as subs]
   [re-frame.core :refer (dispatch subscribe)]))


(defn content []
  (let [ranking @(subscribe [::subs/ranking])]
    [:> mui/Card
     [:> mui/CardHeader {:title "Ranking"}]
     [:> mui/CardContent
      [:div {:style {:display :flex :justify-content :space-between}}
       [:div "#Rank"]
       [:div "Score"]]
      (for [[i {:keys [username wealth]}] (map-indexed vector ranking)]
        ^{:key username}
        [:div {:style {:display :flex}}
         [:div (str i ". " username)]
         [:div wealth]])]]))

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

(defn init-events []
  (dispatch [::events/query-ranking]))

(defn root-panel [props]
  (init-events)
  [:> (with-styles [panel-style] root) props])
