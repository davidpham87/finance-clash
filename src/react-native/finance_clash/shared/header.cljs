(ns finance-clash.shared.header
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   [shadow.expo :as expo]
   [clojure.string :as str]
   ["expo" :as ex]
   ["@expo/vector-icons" :as evi :rename {Ionicons ion-icons}]
   ["react-native" :as rn]
   ["react-native-paper" :as rnp]
   ["react" :as react]
   ["react-navigation" :as rnav]))

(defn app-bar []
  [:> rnp/Appbar.Header
   #_[:> rnp/Appbar.BackAction {:on-press #(rf/dispatch [:set-active-screen "Card1"])}]
   [:> rnp/Appbar.Action
    {:icon "menu"
     :onPress (fn [] (rf/dispatch [:set-drawer-state :toggle]))}]
   [:> rnp/Appbar.Content {:title "Finance Clash"}]])

(defn header []
  [:> rn/View
   [:> rn/StatusBar
    {:backgroundColor :red :hidden false
     :translucent true}]
   [app-bar]])


