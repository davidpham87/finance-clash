(ns finance-clash.router
  (:require
   ["react-navigation" :as rnav]
   ["@expo/vector-icons" :as evi :rename {Ionicons ion-icons}]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [finance-clash.views :refer [routes routes-alternative]]))


(let [active-screen @(rf/subscribe [:active-screen])]
  (def tab-navigator
    (rnav/createBottomTabNavigator
     (clj->js routes)
     (if (routes (keyword active-screen))
       (clj->js {:initialRouteName active-screen})
       (clj->js {}))))

  (def tab-navigator-2
    (rnav/createBottomTabNavigator
     (clj->js routes-alternative)
     (if (routes-alternative (keyword active-screen))
       (clj->js {:initialRouteName active-screen})
       (clj->js {})))))

(defn icon [name]
  (fn [m]
    (reagent/as-element [:> ion-icons {:name name :size 32 :color (.-tintColor m)}])))

(def drawer
  (rnav/createDrawerNavigator
   (clj->js
    {:General
     {:screen tab-navigator
      :navigationOptions
       {:drawerIcon (icon "ios-stats")}}
     :Notification
     {:screen tab-navigator-2
      :navigationOptions {:drawerIcon (icon "ios-notifications")}}})))

(def app-container (rnav/createAppContainer drawer))
