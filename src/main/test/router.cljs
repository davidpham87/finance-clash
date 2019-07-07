(ns test.router
  (:require ["react-navigation" :as rnav]
            [re-frame.core :as rf]
            [test.views :refer [routes routes-alternative]]))


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

(def drawer (rnav/createDrawerNavigator
             (clj->js
              {:General {:screen tab-navigator}
               :Notification {:screen tab-navigator-2}})))

(def app-container (rnav/createAppContainer drawer))


