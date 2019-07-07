(ns test.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   [shadow.expo :as expo]
   [clojure.string :as str]
   ["expo" :as ex]
   ["@expo/vector-icons" :as evi :rename {Ionicons ion-icons}]
   ["react-native" :as rn]
   ["react-native-elements" :as rne]
   ["react" :as react]
   ["react-native-paper" :as rnp]
   ["react-navigation" :as rnav]))

(defonce splash-img (js/require "../assets/shadow-cljs.png"))

(def styles
  ^js (-> {:container
           {:flex 1
            :backgroundColor "#000"
            :alignItems "center"
            :justifyContent "center"
            :padding 12}
           :title
           {:fontWeight "bold"
            :fontSize 24
            :color "skyblue"}}
          (clj->js)
          (rn/StyleSheet.create)))

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

(defn app-bar []
  [:> rnp/Appbar.Header
   [:> rnp/Appbar.BackAction {:on-press #(rf/dispatch [:set-active-screen "Card1"])}]
   [:> rnp/Appbar.Action
    {:icon "menu"
     :onPress
     (fn []
       (rf/dispatch [:set-drawer-state :toggle]))}]
   [:> rnp/Appbar.Content {:title "Examples"}]])

(defn drawer []
  (let [drawer-open? (rf/subscribe [:drawer-open?])]
    (fn []
      [:<>
       (when @drawer-open?
         [:> rn/View
          [:> rn/Text {:style {:color "white"}} "Hellooooo"]
          [:> rnp/Drawer.Section {:style {:backgroundColor "blue"}}
           [:> rnp/Drawer.Item
            {:label "What?!" :icon "inbox" :key 0 :style {:backgroundColor "blue"}
             :onPress #(rf/dispatch [:toggle-drawer])}]]])])))

(defonce count-click (reagent/atom 0))

(def some-long-content
  "The Abandoned Ship is a wrecked ship located on Route 108 in  Hoenn, originally being a ship named the S.S. Cactus. The second  part of the ship can only be accessed by using Dive and contains the Scanner.")

(defn header []
  [:> rn/View {:style {:flex 1}}
   [:> rn/StatusBar {:backgroundColor :red :hidden false}]
   [app-bar]
   [drawer]])

(defn card-screen-1 []
  [:> rn/ScrollView {:style {:flex 1 :backgroundColor "#000"}}
   [header]
   [:> ion-icons {:name "ios-eye" :size 32 :color "white"}]
   [:> rne/Icon {:name "account-circle" :size 32 :color "white"}]
   [card {:card-title "Card ???" :card-subtitle "Subtitle"
          :content "Hello" :title "What?!"}]])

(defn card-screen-2 []
  [:> rn/ScrollView {:style {:flex 1 :backgroundColor "#000"}}
   [header]
   [:<>
    [card {:card-title "Hello!!" :card-subtitle "Yeah!!!"
           :content some-long-content :title "Yep!"}]
    [card {:card-title "2" :card-subtitle "Yeah!!!"
           :content "helll!" :title "Yep this!"}]
    [card {:card-title "3" :card-subtitle "Yeah!!!"
           :content "helll!" :title "Yep!"}]
    [card {:card-title "4" :card-subtitle "Yeah!!!"
           :content "helll!" :title "Yep!"}]]])

(defn home []
  [:> rn/View {:style (.-container styles)}
    [:> rn/Text {:style (.-title styles)}
     "Hello World!! \nThis is crazy fast!! Really\nLet's check!!!"]
    [:> rn/Image {:source splash-img :style {:width 200 :height 200}}]
    [:> rn/View {:style {:padding 12}}
     [:> rnp/Button
      {:mode :contained
       :onPress
       (fn []
         (swap! count-click inc)
         (.alert rn/Alert "Sure" "Message! What?!?"
                 (clj->js [{:text "Cancel"}, {:text "OK"}]),
                 {:cancelable true}))}
      (str "Button Sure: " @count-click)]]])

(defn tab-icon [name]
  (fn [m]
    (reagent/as-element [:> ion-icons {:name name :size 32 :color (.-tintColor m)}])))

(def routes
  {:Home {:navigationOptions
          {:tabBarLabel
           (fn [m]
             (let [{:keys [focused tintColor]} (js->clj m :keywordize-keys true)]
               (reagent/as-element
                [:> rn/Text
                 {:style {:margin-bottom 1 :font-size 12 :textAlign "center"
                          :color tintColor}}
                 (if focused "Home!" "Home")])))
           :tabBarIcon (tab-icon "ios-apps")}
          :screen
          (reagent/reactify-component
           (fn []
             (rf/dispatch [:register-active-screen "Home"])
             [home]))}
   :Card1 {:navigationOptions
           {:title "Card 1"
            :tabBarIcon (tab-icon "ios-chatboxes")}
           :screen
           (reagent/reactify-component
            (fn []
              (rf/dispatch [:register-active-screen "Card1"])
              [card-screen-1]))}
   :Card2 {:navigationOptions
           {:title "Card 2"
            :tabBarIcon (tab-icon "ios-eye")}
           :screen
           (reagent/reactify-component
            (fn []
              (rf/dispatch [:register-active-screen "Card2"])
              [card-screen-2]))}})


(def routes-alternative
  {:Home2 {:navigationOptions
          {:tabBarLabel
           (fn [m]
             (let [{:keys [focused tintColor]} (js->clj m :keywordize-keys true)]
               (reagent/as-element
                [:> rn/Text
                 {:style {:margin-bottom 1 :font-size 12 :textAlign "center"
                          :color tintColor}}
                 (if focused "Home-2!" "Home-2")])))
           :tabBarIcon (tab-icon "ios-eye")}
          :screen
          (reagent/reactify-component
           (fn []
             (rf/dispatch [:register-active-screen "Home2"])
             [home]))}
   :Card3 {:navigationOptions
           {:title "Card 1"
            :tabBarIcon (tab-icon "ios-alert")}
           :screen
           (reagent/reactify-component
            (fn []
              (rf/dispatch [:register-active-screen "Card3"])
              [card-screen-1]))}
   :Card4 {:navigationOptions
           {:title "Card 2"
            :tabBarIcon (tab-icon "ios-apps")}
           :screen
           (reagent/reactify-component
            (fn []
              (rf/dispatch [:register-active-screen "Card4"])
              [card-screen-2]))}})

(comment
  (def navigator @(rf/subscribe [:navigator]))
  (.dispatch navigator (.navigate rnav/NavigationActions #js {:routeName "Card4"}))
  (.dispatch navigator (.navigate rnav/NavigationActions #js {:routeName "Card3"}))
  (.dispatch navigator (.navigate rnav/NavigationActions #js {:routeName "Home"}))

  (do (in-ns 'shadow.user)
      (shadow/repl :app))
  (js/alert "Test"))

