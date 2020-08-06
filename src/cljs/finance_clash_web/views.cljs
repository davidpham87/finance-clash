(ns finance-clash-web.views
  (:require
   ["@material-ui/core" :as mui]
   ["react" :as react]
   [finance-clash-web.admin.core :rename {root-panel admin-panel}]
   [finance-clash-web.chapter-selection.core :rename {root-panel chapter-selection-panel}]
   [finance-clash-web.components.app-bar :refer (app-bar)]
   [finance-clash-web.components.drawer :refer (drawer)]
   [finance-clash-web.components.mui-utils :refer (custom-theme)]
   [finance-clash-web.login.core :rename {root-panel login-panel}]
   [finance-clash-web.quiz.core :rename {root-panel quiz-panel}]
   [finance-clash-web.ranking.core :rename {root-panel ranking-panel}]
   [re-frame.core :as rf :refer (subscribe)]
   [reagent.core :as reagent]))

(defmulti active-panel identity :default :login)
(defmethod active-panel :login [args]
  [login-panel])

(defmethod active-panel :chapter-selection [_]
  [chapter-selection-panel])

(defmethod active-panel :quiz [_]
  [quiz-panel])

(defmethod active-panel :ranking [_]
  [ranking-panel])

(defmethod active-panel :admin [_]
  [admin-panel])


(defn app []
  [:div {:style {:display "flex"}}
   [:> mui/CssBaseline]
   [:> mui/MuiThemeProvider {:theme custom-theme}
    [:> app-bar]
    [:div {:style {:display "flex"}}
     [:> drawer (clj->js {:user-role @(subscribe [:user-role])})]]
    [:> react/Suspense
     {:fallback (reagent/as-element [:div {:style {:height "100vh"}} "Loading"])}
     [active-panel @(subscribe [:active-panel])]]]])
