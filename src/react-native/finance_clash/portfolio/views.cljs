(ns finance-clash.portfolio.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   [shadow.expo :as expo]
   [clojure.string :as str]
   [finance-clash.shared.header :refer [header]]
   ["expo" :as ex]
   ["@expo/vector-icons" :as evi :rename {Ionicons ion-icons}]
   ["react-native" :as rn]
   ["react-native-elements" :as rne]
   ["react" :as react]
   ["react-native-paper" :as rnp]
   ["react-navigation-material-bottom-tabs" :refer (createMaterialBottomTabNavigator)]
   ["react-navigation" :as rnav]))

(defonce background-img (js/require "../assets/portfolio.jpg"))
(defonce ranks-img (js/require "../assets/ranks.jpg"))
(defonce splash-img (js/require "../assets/shadow-cljs.png"))

(def styles
  ^js (-> {:container
           {:flex 1
            :alignItems "center"
            :justifyContent "center"
            :padding 12}
           :title
           {:fontWeight "bold"
            :fontSize 24
            :color "skyblue"}}
          (clj->js)
          (rn/StyleSheet.create)))

(defonce count-click (reagent/atom 0))

(defn home []
  [:> rn/ImageBackground
   {:source background-img :style {:width "100%" :height "100%"}}
   [header]
   [:> rn/View {:style (.-container styles)}
    [:> rn/Text {:style (.-title styles)} "Finance Clash"]
    [:> rn/Image {:source splash-img :style {:width 150 :height 150}}]
    [:> rn/View {:style {:padding 12}}
     [:> rnp/Button
      {:mode :contained
       :onPress
       (fn []
         (swap! count-click inc)
         #_(.alert rn/Alert "Sure" "Message! What?!?"
                 (clj->js [{:text "Cancel"}, {:text "OK"}]),
                 {:cancelable true}))}
      (str "Button Sure: " @count-click)]]]])

(defn ranks []
  [:> rn/ImageBackground
   {:source ranks-img :style {:width "100%" :height "100%"}}
   [header]
   [:> rn/View {:style (.-container styles)}
    [:> rn/Text {:style (.-title styles)} "Finance Clash"]
    [:> rn/Image {:source splash-img :style {:width 150 :height 150}}]
    [:> rn/View {:style {:padding 12}}
     [:> rnp/Button
      {:mode :contained
       :onPress
       (fn []
         (swap! count-click inc)
         (.alert rn/Alert "Sure" "Message! What?!?"
                 (clj->js [{:text "Cancel"}, {:text "OK"}]),
                 {:cancelable true}))}
      (str "Button Sure: " @count-click)]]]])


(defn ->ion-icon [name]
  (fn [m]
    (reagent/as-element [:> ion-icons {:name name :size 20 :color (.-tintColor m)}])))

(def routes
  {::portfolio
   {:navigationOptions
    {:tabBarLabel
     (reagent/as-element
      [(fn [m]
        (let [{:keys [focused tintColor]} (js->clj m :keywordize-keys true)]
          (reagent/as-element
           [:> rn/Text
            {:style {:margin-bottom 1 :font-size 12 :textAlign "center"
                     :color tintColor}} "Portfolio"])))])
     :tabBarIcon (->ion-icon "ios-apps")}
    :screen
    (reagent/reactify-component
     (fn []
       (rf/dispatch [:register-active-screen ::portfolio])
       [home]))}
   ::investment
   {:navigationOptions
    {:title "Investment"
     :tabBarIcon (->ion-icon "ios-chatboxes")}
    :screen
    (reagent/reactify-component
     (fn [] (rf/dispatch [:register-active-screen ::investment])
       [home]))}
   ::ranking
   {:navigationOptions
    {:title "Ranking"
     :tabBarIcon (->ion-icon "ios-eye")}
    :screen
    (reagent/reactify-component
     (fn []
       (rf/dispatch [:register-active-screen ::ranking])
       [ranks]))}})

(let [active-screen @(rf/subscribe [:active-screen])]
  (defn tab-navigator []
    (rnav/createBottomTabNavigator
     #_createMaterialBottomTabNavigator
     (clj->js (reduce-kv #(assoc %1 (str %2) %3) {} routes))
     (if (routes (keyword active-screen))
       (clj->js {:initialRouteName (str active-screen)})
       (clj->js {})))))

(comment
  (rf/dispatch [:set-active-screen ::investment])
  @(rf/subscribe [:active-screen])
  (.log js/console @(rf/subscribe [:navigator])))
