(ns finance-clash.questions.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   [shadow.expo :as expo]
   [clojure.string :as str]
   [finance-clash.shared.header :refer [header]]
   [finance-clash.questions.events :as events]
   [finance-clash.questions.subs :as sub]
   [cljs-time.core :as ct]
   [cognitect.transit :as t]
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
(defonce data
  {:0 (js/require "../assets/questions/0_key_notions.json")
   :3 (js/require "../assets/questions/3_le_bilan.json")})

(def some-long-content
  "The Abandoned Ship is a wrecked ship located on Route 108 in Hoenn,
  originally being a ship named the S.S. Cactus. The second part of the ship can
  only be accessed by using Dive and contains the Scanner.")

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

(defn quiz-button
  [{:keys [response selected? selected-answer id]
    :or {selected? false}}]
  (fn [{:keys [response selected? selected-answer id]
          :or {selected? false}}]
      [:> rn/TouchableOpacity
       {:style {:background-color
                (if-not selected? #_(and @selected-internal? selected?)
                  "rgba(255, 0, 0, 0.8)"
                  "rgba(80, 0, 0, 0.8)")
                :justifyContent :center
                :alignItems :center
                :border-radius 30
                :min-height "10%"}
        :on-press
        (fn []
          (reset! selected-answer id))}
       [:> rn/Text {:style {:color :white
                            :text-align :center
                            :padding 10}}
        response]]))


(defn timer [duration]
  (let [timer (reagent/atom duration)]
    (rf/dispatch [::events/log-question-action 0 :start (js/Date.)])
    (fn [duration]
      (if (pos? @timer)
        (js/setTimeout #(swap! timer dec) 1000)
        (rf/dispatch [::events/log-question-action 0 :end (js/Date.)]))
      [:> rn/Text {:style {:color :white}}
       (str "Remaining time: " (max @timer 0))])))

(defn question-card [m]
  (let [selected-answer (reagent/atom nil)]
    (fn [{:keys [question responses correct-response duration difficulty]}]
    [:> rn/View {:style {:height "99%"}}
     [title question]
     [:> rnp/Card.Content {:style {:margin 0 :opacity 1 :flex 1
                                   :justifyContent :space-around}}
      (doall
       (for [idx (range (count responses))
             :let [response (nth responses idx)]]
         ^{:key idx}
         [quiz-button {:selected? (= @selected-answer idx) :response response
                       :id idx
                       :selected-answer selected-answer}]))]

     [:> rnp/Card.Actions {:style {:justifyContent :space-between}}
      [:> rn/View {:style {:justify-content :space-between
                           :flex 1
                           :flex-direction :row}}
       [:> rnp/Button {:mode :outlined :disabled true}
        [timer duration]]
       [:> rnp/Button {:mode :contained :on-press
                       #(js/alert (str "Hello" (when @selected-answer (str ": " (inc @selected-answer)))))}
        (str "Validate" (when @selected-answer (str ": " (inc @selected-answer))))]]]])))

(defn question-quiz []
  (let [questions (js->clj data :keywordize-keys true)
        #_@(rf/subscribe [::sub/questions 1])]
    [:> rn/ImageBackground {:source daily-questions-img
                            :style {:width "100%" :height "100%"}}
     [header]
     [:> rn/View {:style {:flex 1 :justifyContent :space-around}}
      (let [question-spec (rand-nth (rand-nth (vals questions)))]
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
         #_(.log js/console
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



(comment
  (t/read (t/reader :json) (t/write (t/writer :json) (js/Date.)))
  (def x (t/read (t/reader :json) (t/write (t/writer :json) (js/Date.))))
  

  )
