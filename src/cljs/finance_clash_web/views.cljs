(ns finance-clash-web.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as rf :refer (dispatch subscribe)]
   [finance-clash-web.components.mui-utils :refer (custom-theme)]
   [finance-clash-web.components.app-bar :refer (app-bar)]
   [finance-clash-web.login.core :rename {root-panel login-panel}]
   [finance-clash-web.chapter-selection.core :rename {root-panel chapter-panel}]
   [finance-clash-web.quizz.core :rename {root-panel quizz-panel}]
   ["@material-ui/core" :as mui]
   ["react" :as react]))

(defn app []
  [:div {:style {:display "flex"}}
   [:> mui/CssBaseline]
   [:> mui/MuiThemeProvider {:theme custom-theme}
    [:> app-bar]
    #_[user-feedback-comp]
    #_[:div {:style {:display "flex"}}
     [:> drawer (clj->js {:user-role (<sub [:user-role])})]]
    [:> react/Suspense
     {:fallback (reagent/as-element [:div {:style {:height "100vh"}} "Loading"])}
     [quizz-panel]
     #_[active-panel (<sub [:active-panel])]]]])
