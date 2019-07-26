(ns finance-clash.shared.bottom-nav
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   [goog.object :as gobj]
   ["react-native-paper" :as rnp]
   ["react-navigation" :as rnav]))

(defn get-color [route descriptors]
  (-> (gobj/get descriptors (.-key route))
      .-options
      .-tabBarColor))

(defn visible? [navigation descriptors]
  (let [state (.-state navigation)
        route (-> state (gobj/get (.-routes state) (.-index state)))]
    (-> (goog/get descriptors (.-key route)) .-options .-tabColor)))

(defn render-icon [{:keys [route focused color]}]
  (let [props (-> reagent/current-component reagent/props)]
    (.renderIcon (js->clj {:route route :focused focused :tintColor color}))))

(defn bottom-navigation-view
  [{:keys [active-color routes inactive-color navigation-state descriptor bar-style]} :as m]
  (let [this (reagnet/current-component)
        props (reagent/props this)
        descriptors (.-descriptors props) ]
    [:> rnp/BottomNavigation
     {:activeColor active-color
      :inactiveColor inactive-color
      :renderIcon render-icon
      :navigationState navigation-state
      :barStyle [bar-style extra-style]
      :getColor (fn [{route :route}] (get-color route descriptors))}]))

;;       <BottomNavigation
;;         activeColor={activeTintColor}
;;         inactiveColor={inactiveTintColor}
;;         {...rest}
;;         renderIcon={this._renderIcon}
;;         barStyle={[barStyle, extraStyle]}
;;         navigationState={navigation.state}
;;         getColor={this._getColor}
;;       />
