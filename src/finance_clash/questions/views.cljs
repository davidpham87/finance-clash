(ns finance-clash.questions.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   [shadow.expo :as expo]
   [clojure.string :as str]
   [finance-clash.shared.header :refer [header]]
   ["expo" :as ex]
   ["@expo/vector-icons" :as evi :rename {Ionicons ion-icons}]
   ["react-native" :as rn]
   ["react" :as react]
   ["react-native-paper" :as rnp]
   ["react-navigation" :as rnav]))

(defonce splash-img (js/require "../assets/shadow-cljs.png"))
(defonce daily-questions-img (js/require "../assets/daily_questions.jpg"))
(defonce ranking-img (js/require "../assets/ranks.jpg"))

(def some-long-content
  "The Abandoned Ship is a wrecked ship located on Route 108 in  Hoenn, originally being a ship named the S.S. Cactus. The second  part of the ship can only be accessed by using Dive and contains the Scanner.")

(defn card [m]
  (let [pic-number (reagent/atom (rand-int 1000))]
    (fn [{:keys [card-title card-subtitle title content cover]}]
      [:> rnp/Card {:style {:margin 10} :justifyContent :center}
       [:> rnp/Card.Title {:title card-title :subtitle card-subtitle
                           :left #(reagent/as-element [:> rnp/Avatar.Icon {:size 48 :icon "folder"}])}]
       [:> rnp/Card.Cover
        {:flex 1
         :source {:uri (clojure.core/str "https://picsum.photos/" @pic-number)}}]
       [:> rnp/Card.Content {:style {:margin 0}}
        (when title [:> rnp/Title title])
        [:> rnp/Paragraph content]]
       [:> rnp/Card.Actions
        [:> rnp/Button "Cancel this!"]
        [:> rnp/Button {:mode :contained
                        :onPress #(reset! pic-number (rand-int 1000))} "OK"]]])))


(defn card-screen-1 []
  [:> rn/ImageBackground {:source daily-questions-img
                          :style {:width "100%" :height "100%"}}
   [:> rn/ScrollView {:style {:flex 1}}
    [header]
    [card {:card-title "Card" :card-subtitle "Subtitle"
           :content "Hello" :title "What?!"}]]])

(defn card-screen-2 []
  [:> rn/ImageBackground {:source ranking-img :style {:width "100%" :height "100%"}}
   [:> rn/ScrollView {:style {:flex 1}}
   [header]
   [:<>
    [card {:card-title "Hello!!" :card-subtitle "Yeah!!!"
           :content some-long-content :title "Yep!"}]
    [card {:card-title "2" :card-subtitle "Yeah!!!"
           :content "helll!" :title "Yep this!"}]
    [card {:card-title "3" :card-subtitle "Yeah!!!"
           :content "helll!" :title "Yep!"}]
    [card {:card-title "4" :card-subtitle "Yeah!!!"
           :content "helll!" :title "Yep!"}]]]])

(defn ->ion-icon [name]
  (fn [m]
    (reagent/as-element [:> ion-icons {:name name :size 32 :color (.-tintColor m)}])))

(def routes
  {::summary
   {:navigationOptions
    {:tabBarLabel
     (fn [m]
       (let [{:keys [focused tintColor]} (js->clj m :keywordize-keys true)]
         (reagent/as-element
          [:> rn/Text
           {:style {:margin-bottom 1 :font-size 12 :textAlign "center"
                    :color tintColor}}
           "Summary"])))
     :tabBarIcon (->ion-icon "ios-eye")}
    :screen
    (reagent/reactify-component
     (fn []
       (rf/dispatch [:register-active-screen ::summary])
       [card-screen-1]))}
   ::questions
   {:navigationOptions
    {:title "Daily Questions"
     :tabBarIcon (->ion-icon "ios-alert")}
    :screen
    (reagent/reactify-component
     (fn []
       (rf/dispatch [:register-active-screen ::question])
       [card-screen-2]))}})


(def tab-navigator
  (let [active-screen @(rf/subscribe [:active-screen])]
    (rnav/createBottomTabNavigator
     (clj->js routes {:keyword-fn str})
     (if (routes (keyword active-screen))
       (clj->js {:initialRouteName active-screen})
       (clj->js {})))))
