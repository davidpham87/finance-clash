(ns finance-clash-web.components.drawer
  (:require
   [finance-clash-web.components.mui-utils :refer
    [custom-theme cs with-styles mui-list-item]]
   [finance-clash-web.components.colors :as colors]
   [clojure.string :as str]
   [reagent.core :as reagent]
   [re-frame.core :as rf :refer [dispatch subscribe]]
   [goog.object :as gobj]

   ["@material-ui/core" :as mui]
   ["@material-ui/icons/AttachMoney" :default ic-attach-money]
   ["@material-ui/icons/Assessment" :default ic-assessment]
   ["@material-ui/icons/BarChart" :default ic-bar-chart]
   ["@material-ui/icons/Check" :default ic-check]
   ["@material-ui/icons/ChevronLeft" :default ic-chevron-left]
   ["@material-ui/icons/ChevronRight" :default ic-chevron-right]
   ["@material-ui/icons/EuroSymbol" :default ic-euro-symbol]
   ["@material-ui/icons/EvStation" :default ic-ev-station]
   ["@material-ui/icons/ExpandMore" :default ic-expand-more]
   ["@material-ui/icons/ExpandLess" :default ic-expand-less]
   ["@material-ui/icons/Forum" :default ic-forum]
   ["@material-ui/icons/Dashboard" :default ic-dashboard]
   ["@material-ui/icons/Explore" :default ic-explore]
   ["@material-ui/icons/Home" :default ic-home]
   ["@material-ui/icons/Input" :default ic-input]
   ["@material-ui/icons/Layers" :default ic-layers]
   ["@material-ui/icons/Note" :default ic-note]
   ["@material-ui/icons/Person" :default ic-person]
   ["@material-ui/icons/People" :default ic-people]
   ["@material-ui/icons/PieChart" :default ic-pie-chart]
   ["@material-ui/icons/Public" :default ic-public]
   ["@material-ui/icons/Settings" :default ic-settings]
   ["@material-ui/icons/ShoppingCart" :default ic-shopping-cart]
   ["@material-ui/icons/TrendingUp" :default ic-trending-up]
   ["@material-ui/icons/Update" :default ic-update]
   ["@material-ui/icons/ZoomIn" :default ic-zoom-in]))

(defn drawer-style [theme]
  (let [drawer-width 280
        transitions (.-transitions theme)
        duration (.-duration transitions)
        easing-sharpe (.. transitions -easing -sharp)
        spacing-unit (.. theme -spacing)
        breakpoint-sm-up ((.. theme -breakpoints -up) "sm")]

    #js {:drawerPaper
         #js {:position "relative"
              :white-space "nowrap"
              :width drawer-width
              :flexShrink 0
              :height "100vh"
              :transition
              (.create transitions "width"
                       #js {:easing easing-sharpe
                            :duration (.-enteringScreen duration)})}

         :drawerPaperClose
         #js {:paper #js {:background (colors/colors-rgb :main)
                          :color (.. theme -palette -common -white)}
              :overFlowX "hidden"
              :position "relative"
              :transition
              (.create transitions "width"
                       #js {:easing easing-sharpe
                            :duration (.-leavingScreen duration)})
              :width (spacing-unit 8)
              "@media (min-width:600px)"
              #js {:width (spacing-unit 9)}}

         :drawerPaperPaperProps
         #js {:background (colors/colors-rgb :main)
              :color (.. custom-theme -palette -common -white)
              :position "relative" :overflowX "hidden"
              :height "100vh"
              :transition
              (.create transitions "width"
                       #js {:easing easing-sharpe
                            :duration (.-enteringScreen duration)})}

         :toolbarIcon (.assign js/Object
                               (.. theme -mixins -toolbar)
                               #js {:display "flex"
                                    :align-items "center"
                                    :justify-content "flex-end"
                                    :padding "0px 8px"
                                    :width "100%"})
         :divider #js {:background-color "rgba(255, 255, 255, 0.12)"}}))

(defn list-divider [classes]
  (let [divider-class (cs (gobj/get classes "divider"))]
    [:> mui/Divider {:class divider-class}]))

(defn mui-list-item-std
  [[icon text] {:keys [dispatch-event style tooltip?]
                :or {tooltip? false} :as m}]
  (let [active-panel (subscribe [:active-panel])
        item-id (second dispatch-event)
        bg-color (-> @active-panel (= item-id) (if :silver-dark :main) (colors/colors-rgb))]
    [mui-list-item [icon text] (merge m {:style {:background-color bg-color}})]))

(defn mui-list-item-root
  [[icon text] {:keys [dispatch-event style open? tooltip?]
                :or {tooltip? false}}]
  (let [list-item-root
        [:> mui/ListItem
         {:button true
          :on-click (when dispatch-event #(rf/dispatch dispatch-event))
          :style (merge {:background-color (colors/colors-rgb :main)} style)}
         (when icon [:> mui/ListItemIcon {:style {:padding-left "8px"}}
                     [:> icon {:style {:color "white"}}]])
         [:> mui/ListItemText {:primaryTypographyProps #js {:style #js {:color "white"}}
                               :primary text}]
         [:> (if open? ic-expand-less ic-expand-more) {:style {:z-index 10 :color "white"}}]]]
    (if tooltip?
      [:> mui/Tooltip {:title text :placement :bottom-start} list-item-root]
      list-item-root)))

(defn mui-list-item-nested
  [[icon text] {:keys [dispatch-event style tooltip?] :as m}]
  (let [active-panel (rf/subscribe [:active-panel])
        item-id (second dispatch-event)
        bg-color (-> (if (= @active-panel item-id) :coral-dark :coral)
                     colors/colors-rgb)]
    [mui-list-item [icon text]
     {:tooltip? tooltip?
      :dispatch-event dispatch-event
      :style (merge {:background-color bg-color} style)}]))

(def panels-label
  "Path displayed for navigation"
  {:login "Login"
   :chapter-selection "Module Selection"
   :quizz "Quizz"})

(defn tabs-public [user-role drawer-open? classes]
  [:<>
   [mui-list-item-std [ic-home "Home"]
    {:dispatch-event [:set-active-panel :login]}]
   [mui-list-item-std [ic-explore "Questions"]
    {:dispatch-event [:set-active-panel :quizz]}]
   [mui-list-item-std [ic-dashboard "Modules"]
    {:dispatch-event [:set-active-panel :chapter-selection]}]])

(defn drawer-react
  "The main components of the drawer. Refactor this tab to provides the tabs as
  argument."
  [{:keys [classes] :as props}]
  (let [drawer-open? (subscribe [:drawer-open?])
        user-role (subscribe [:user-role])]
    (fn [{:keys [classes] :as props}]
      (let [user-role (or @user-role #{:user})
            tabs [[tabs-public user-role @drawer-open? classes]]]
        [:> mui/Drawer
         {:open @drawer-open?
          :class (cs (.-drawerPaper classes)
                     (when-not @drawer-open? (.-drawerPaperClose classes)))
          :ModalProps #js {:onBackdropClick #(dispatch [:close-drawer])}
          :PaperProps #js {:class (cs (gobj/get classes "drawerPaperPaperProps"))}}
         [:div {:style {:width "280px"}}
          [:div {:class (.-toolbarIcon classes)}
           [:> mui/IconButton {:onClick #(dispatch [:toggle-drawer])}
            [:> ic-chevron-left {:style {:color "white"}}]]]
          [list-divider classes]
          [:> mui/List {:style {:padding-top 0}}
           (into [:div] tabs)]]]))))

(def drawer (with-styles [drawer-style] drawer-react))

(comment
  (dispatch [:user/login {:email "d@vescore.com" :password "d"}])
  (dispatch [:user/logout]))
