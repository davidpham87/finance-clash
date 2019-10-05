(ns finance-clash-web.components.app-bar
  (:require
   [finance-clash-web.components.mui-utils :refer (cs custom-theme with-styles)]
   [finance-clash-web.components.timer :as timer-comp]
   [clojure.string :as str]
   [reagent.core :as reagent]
   [re-frame.core :as rf :refer [dispatch subscribe]]

   ["@material-ui/core" :as mui]
   ["@material-ui/icons/AccountCircle" :default ic-account-circle]
   ["@material-ui/icons/Help" :default ic-help]
   ["@material-ui/icons/Settings" :default ic-settings]
   ["@material-ui/icons/Menu" :default ic-menu]
   ["@material-ui/icons/Notifications" :default ic-notification]
   ["@material-ui/icons/Person" :default ic-person]))

(defn app-bar-style [theme]
  (let [drawer-width 280
        transitions (.. theme -transitions)
        easing-sharp (.. transitions -easing -sharp)
        duration (.. transitions -duration)]

    #js {:appBar
         #js {:zIndex (+ (.. theme -zIndex -drawer) 1),
              :padding 0
              :transition
              (.create transitions
                       #js ["width" "margin"]
                       #js {:easing easing-sharp
                            :duration (.-leavingScreen duration)})}
         :appBarShift
         #js {:margin-left drawer-width
              :padding-left "4px"
              :width  (str/join "" ["calc(100% - " drawer-width "px)"])
              :transition
              (.create transitions
                       #js ["width" "margin"]
                       #js {:easing easing-sharp
                            :duration (.-enteringScreen duration)})}}))


;; bad design, should just be hidden
(defn menu-button-style [theme]
  #js {:menuButton #js {:margin-left 12 :margin-right 36}
       :menuButtonHidden #js {:display "none"}})

(defn toolbar-style [theme]
  #js {:toolbar #js {:padding-right 24}
       :title #js {:flex-grow 1}})

(defn app-bar-label
  [active-panel-label timer]
  (cond
    (pos? (:remaining timer 0)) timer
    active-panel-label (clojure.core/str "Finance Clash - "
                                         active-panel-label)
    :else "Finance Clash"))

;; TODO(dph): put the timer here
(defn app-bar-react [{:keys [classes] :as props}]
  (let [drawer-open? (subscribe [:drawer-open?])
        active-panel-label (subscribe [:active-panel-label])
        timer (subscribe [::timer-comp/timer :quiz])]
    (fn [{:keys [classes] :as props}]
      [:> mui/AppBar {:position "absolute"
                      :class (cs (.-appBar classes)
                                 #_(when @drawer-open? (.-appBarShift classes)))
                      :style {:background-color "#ffffff"}}
       [:> mui/Toolbar {:disableGutters (not @drawer-open?)
                        :class (cs (.-toolbar classes))
                        :style {:padding-left 0} #_(when-not @drawer-open? )}
        [:> mui/IconButton
         {:color "primary"
          :class (cs (.-menuButton classes)
                     #_(when @drawer-open? (.-menuButtonHidden classes)))
          :on-click #(dispatch [:toggle-drawer])}
         [:> ic-menu {:color "primary"}]]
        [:> mui/Typography
         {:component "h1" :variant "h6" :color "primary" :no-wrap true
          :class (cs (.-title classes))}
         [app-bar-label @active-panel-label @timer]]
        #_[:> mui/Tooltip {:title "Help"}
         [:> mui/IconButton {:onClick #(dispatch [:set-active-panel help-event])
                             :color "primary"}
          [:> ic-help]]]
        [:> mui/Tooltip {:title "Profile"}
         [:> mui/IconButton {:onClick #(dispatch [:set-active-panel :login])
                             :color "primary"}
          [:> ic-account-circle]]]]])))

(def app-bar
   (with-styles [app-bar-style menu-button-style toolbar-style] app-bar-react))
