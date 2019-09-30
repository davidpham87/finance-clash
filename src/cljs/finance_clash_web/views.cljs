(ns finance-clash-web.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as rf :refer (dispatch subscribe)]
   [finance-clash-web.components.mui-utils :refer (custom-theme)]
   [finance-clash-web.components.app-bar :refer (app-bar)]
   [finance-clash-web.components.drawer :refer (drawer)]
   [finance-clash-web.login.core :rename {root-panel login-panel}]
   [finance-clash-web.chapter-selection.core :rename {root-panel chapter-selection-panel}]
   [finance-clash-web.quiz.core :rename {root-panel quiz-panel}]
   ["@material-ui/core" :as mui]
   ["react" :as react]))

(defmulti active-panel identity :default :login)
(defmethod active-panel :login [args]
  [login-panel])

(defmethod active-panel :chapter-selection [_]
  [chapter-selection-panel])

(defmethod active-panel :quiz [_]
  [quiz-panel])

(defn app []
  [:div {:style {:display "flex"}}
   [:> mui/CssBaseline]
   [:> mui/MuiThemeProvider {:theme custom-theme}
    [:> app-bar]
    #_[user-feedback-comp]
    [:div {:style {:display "flex"}}
     [:> drawer (clj->js {:user-role @(subscribe [:user-role])})]]
    [:> react/Suspense
     {:fallback (reagent/as-element [:div {:style {:height "100vh"}} "Loading"])}
     [active-panel @(subscribe [:active-panel])]]]])
