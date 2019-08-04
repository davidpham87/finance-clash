(ns finance-clash.questions.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   [shadow.expo :as expo]
   [clojure.string :as str]
   [finance-clash.shared.header :refer [header]]
   [finance-clash.questions.events :as events]
   [finance-clash.questions.subs :as sub]
   ["expo" :as ex]
   ["@expo/vector-icons" :as evi :rename {Ionicons ion-icons}]
   ["react-native" :as rn]
   ["react" :as react]
   ["react-native-paper" :as rnp]
   ["react-navigation-material-bottom-tabs" :refer (createMaterialBottomTabNavigator)]
   ["react-navigation" :as rnav]))

(defonce splash-img (js/require "../assets/shadow-cljs.png"))
(defonce daily-questions-img (js/require "../assets/daily_questions.jpg"))
(defonce ranking-img (js/require "../assets/ranks.jpg"))
(defonce data (js/require "../assets/questions/1_intro.json"))

(def some-long-content
  "The Abandoned Ship is a wrecked ship located on Route 108 in  Hoenn, originally being a ship named the S.S. Cactus. The second  part of the ship can only be accessed by using Dive and contains the Scanner.")

(defn card [m]
  (let [pic-number (reagent/atom (rand-int 1000))]
    (fn [{:keys [card-title card-subtitle title content cover]}]
      [:> rnp/Card {:style {:margin 10 :justifyContent :center}}
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

(def sample-question-spec [2 3 4])

(defn title [text]
  [:> rn/Text {:style {:margin 20 :font-weight :bold :font-size 20
                       :color :white}} text])

(defn quiz-button [children]
  [:> rn/TouchableOpacity
   {:style {:background-color "rgba(255, 0, 0, 0.8)"
            :font-color :white
            :justifyContent :center
            :alignItems :center
            :border-radius 30
            :min-height "10%"}}
   [:> rn/Text {:style {:color :white
                        :text-align :center
                        :padding 10}}
    children]])

(defn question-card [m]
  (let [timer (reagent/atom (:duration m))]
    (fn [{:keys [question responses correct-response duration difficulty]}]
      (when (pos? @timer)
        (js/setTimeout #(swap! timer dec) 1000))
    [:> rn/View {:style {:height "87%"}}
     [title question]
     [:> rnp/Card.Content {:style {:margin 0 :opacity 1 :flex 1
                                   :justifyContent :space-around}}
      (for [idx (range (count responses))
            :let [response (nth responses idx)]]
        ^{:key idx}
        [quiz-button response])]

     [:> rnp/Card.Actions {:style {:justifyContent :space-between}}
      [:> rn/View {:style {:justify-content :space-between
                           :flex 1
                           :flex-direction :row}}
       [:> rnp/Button {:mode :outlined :disabled true}
        [:> rn/Text {:style {:color :white}} (str "Remaining time: " @timer)]]
       [:> rnp/Button {:mode :contained :on-press
                       #(.log js/console "Hello")}
        "Validate"]]]]
     #_[:> rnp/Card {:style {:margin 10 :justifyContent :center
                             :backgroud-color "rgba(0,0,0)"
                             :opacity 0.5
                             :height "87%"}}
     #_[:> #_rnp/Card.Title
      rn/Text
      {:title (reagent/as-element [:> rnp/Paragraph question])
       :subtitle difficulty :style {:opacity 1.0}}
      question]
     [title question]

     [:> rnp/Card.Content {:style {:margin 0 :opacity 1 :flex 1
                                   :justifyContent :space-around}}
      (for [idx (range (count responses))
            :let [response (nth responses idx)]]
        ^{:key idx}
        [:> #_rnp/Paragraph
         rn/Button
         {:title response
          :style {:text-align :center :opacity 1}}])]

     [:> rnp/Card.Actions {:style {:alignSelf :flex-end}}
      [:> rnp/Button {:mode :contained :on-press
                      #(.log js/console "Hello")}
       "Validate"]]])))

(defn question-quiz []
  (let [questions (js->clj data :keywordize-keys true)
        #_@(rf/subscribe [::sub/questions 1])]
    [:> rn/ImageBackground {:source daily-questions-img
                            :style {:width "100%" :height "100%"}}
     [:> rn/View {:style {:flex 1 :justifyContent :space-around}}
      [header]

      (let [question-spec (nth questions 1)]
        [question-card question-spec])]]))

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
    [card {:card-title "Hello" :card-subtitle "No!!!"
           :content some-long-content :title "Yep!"}]
    [card {:card-title "2" :card-subtitle "No!!!!"
           :content "What!" :title "Yep this!"}]
    [card {:card-title "3" :card-subtitle "Yeah!!!"
           :content "Helll!" :title "Yep!"}]
    [card {:card-title "Heaven" :card-subtitle "Yeah!!!"
           :content "Heaven!" :title "Yep!"}]]]])

(defn ->ion-icon [name]
  (fn [m]
    (reagent/as-element [:> ion-icons {:name name :size 20 :color (.-tintColor m)}])))

(def routes
  {::summary
   {:navigationOptions
    {:title "Summary"
     :tabBarIcon (->ion-icon "ios-eye")}
    :screen
    (reagent/reactify-component
     (fn []
       (let [active-screen @(rf/subscribe [:active-screen])]
         (.log js/console
               "Activescreen in Summary"
               (str active-screen))
         (when-not (= active-screen ::summary)
           (rf/dispatch [:register-active-screen ::summary])))
       [question-quiz]
       #_[card-screen-1]))}
   ::questions
   {:navigationOptions
    {:title "Daily Questions"
     :tabBarIcon (->ion-icon "ios-alert")}
    :screen
    (reagent/reactify-component
     (fn []
       (rf/dispatch [:register-active-screen ::questions])
       [card-screen-2]))}})

(let [active-screen @(rf/subscribe [:active-screen])]
  (defn tab-navigator []
    (#_createMaterialBottomTabNavigator
     rnav/createBottomTabNavigator
     (clj->js (reduce-kv #(assoc %1 (str %2) %3) {} routes))
     (if (routes (keyword active-screen))
       (clj->js {:initialRouteName (str active-screen)})
       (clj->js {})))))

