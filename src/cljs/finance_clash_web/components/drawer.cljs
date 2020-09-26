(ns finance-clash-web.components.drawer
  (:require
   ["@material-ui/core" :as mui]
   ["@material-ui/icons/BarChart" :default ic-bar-chart]
   ["@material-ui/icons/Build" :default ic-build]
   ["@material-ui/icons/ChevronLeft" :default ic-chevron-left]
   ["@material-ui/icons/Dashboard" :default ic-dashboard]
   ["@material-ui/icons/ExpandLess" :default ic-expand-less]
   ["@material-ui/icons/ExpandMore" :default ic-expand-more]
   ["@material-ui/icons/Explore" :default ic-explore]
   ["@material-ui/icons/Home" :default ic-home]
   ["@material-ui/icons/Settings" :default ic-settings]
   [finance-clash-web.components.colors :as colors]
   [finance-clash-web.components.mui-utils :refer
    [custom-theme cs with-styles mui-list-item]]
   [goog.object :as gobj]
   [re-frame.core :as rf :refer [dispatch subscribe]]))

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
              :height "100%"
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

         :drawerPaperProps
         #js {:background (colors/colors-rgb :main)
              :color (.. custom-theme -palette -common -white)
              :position "relative" :overflowX "hidden"
              :height "100%"
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
   :quiz "Quiz"
   :ranking "Ranking"
   :welcome "Welcome"
   :default "Welcome"
   :admin "Administration"
   :rework "Rework"})

(defn tabs-public []
  [:<> [mui-list-item-std [ic-home "Home"]
        {:dispatch-event [:set-active-panel :login]}]])

(defn tabs-logged [logged?]
  (when logged?
    [:<>
     [mui-list-item-std [ic-explore "Questions"]
      {:dispatch-event [:set-active-panel :quiz]}]
     [mui-list-item-std [ic-bar-chart "Ranking"]
      {:dispatch-event [:set-active-panel :ranking]}]
     [mui-list-item-std [ic-dashboard "Modules"]
      {:dispatch-event [:set-active-panel :chapter-selection]}]]))

(defn tabs-admin [super-user?]
  [:<>
   (when super-user?
     [:<>
      [mui-list-item-std [ic-settings "Admin"]
           {:dispatch-event [:set-active-panel :admin]}]
      [mui-list-item-std [ic-build "Rework"]
       {:dispatch-event [:set-active-panel :rework]}]])])

(defn drawer-react
  "The main components of the drawer. Refactor this tab to provides the tabs as
  argument."
  [{:keys [classes] :as props}]
  (let [drawer-open? (subscribe [:drawer-open?])
        user-logged? (subscribe [:user-logged?])
        super-user? (subscribe [:super-user?])]
    (fn [{:keys [classes] :as props}]
      (let [tabs [[tabs-public]
                  [tabs-logged @user-logged?]
                  [tabs-admin @super-user?]]]
        [:> mui/Drawer
         {:open @drawer-open?
          :variant :temporary
          :className (cs (gobj/get classes "drawerPaper"))
          :ModalProps #js {:onBackdropClick #(dispatch [:close-drawer])}
          :PaperProps #js {:className (cs (gobj/get classes "drawerPaperProps"))}}
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
