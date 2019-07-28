(ns finance-clash.views
  (:require
   ["react-navigation" :as rnav]
   ["@expo/vector-icons" :rename {Ionicons ion-icons
                                  MaterialIcons material-icons}]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [finance-clash.portfolio.views :rename {tab-navigator portfolio-navigator}]
   [finance-clash.questions.views :rename {tab-navigator questions-navigator}]))

(defn ->ion-icon [name]
  (fn [m]
    (reagent/as-element [:> ion-icons {:name name :size 32 :color (.-tintColor m)}])))

(defn ->material-icon [name]
  (fn [m]
    (reagent/as-element [:> material-icons {:name name :size 32
                                            :color (.-tintColor m)}])))

(defn drawer []
  (let [active-screen (rf/subscribe [:active-screen])
        choice (when @active-screen
                 (-> @active-screen namespace
                     (clojure.string/split #"\.") butlast last))]
    (rnav/createDrawerNavigator
     (clj->js
      {:portfolio
       {:getScreen portfolio-navigator
        :navigationOptions
        {:drawerLabel "Portfolio"
         :drawerIcon (->ion-icon "ios-stats")}}
       :questions
       {:getScreen questions-navigator
        :navigationOptions
        {:drawerLabel "Questions"
         :drawerIcon (->material-icon "question-answer")}}})
     (clj->js (if choice {:initialRouteName choice} {})))))

(def app-container (rnav/createAppContainer (drawer)))

(comment
  (rf/dispatch [:set-active-screen "settings/card"])
  (def navigator @(rf/subscribe [:navigator]))
  @(rf/subscribe [:active-screen])
  (.dispatch navigator (.navigate rnav/NavigationActions #js {:routeName "settings/home"}))
  (.dispatch navigator (.navigate rnav/NavigationActions #js {:routeName "Card3"}))
  (.dispatch navigator (.navigate rnav/NavigationActions #js {:routeName "Card1"}))

  (do (in-ns 'shadow.user)
      (shadow/repl :app))
  (js/alert "Test"))

