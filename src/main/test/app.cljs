(ns test.app
  (:require
    ["expo" :as ex]
    ["react-native" :as rn]
    ["react" :as react]
    [reagent.core :as reagent]
    [re-frame.core :as rf]
    [shadow.expo :as expo]
    ["react-native-paper" :as rnp]
    ["react-navigation" :as rnav]
    [clojure.string :as str]
    [test.events]
    [test.subs]
    [test.router :refer [app-container]]))

;; must use defonce and must refresh full app so metro can fill these in
;; at live-reload time `require` does not exist and will cause errors
;; must use path relative to :output-dir

(defn root []
  (let [navigator (rf/subscribe [:navigator])
        active-screen (rf/subscribe [:active-screen])]
    (fn []
      (when (and @navigator @active-screen)
        (.dispatch @navigator
                   (.navigate rnav/NavigationActions #js {:routeName @active-screen})))
      [:> app-container {:ref #(rf/dispatch [:set-navigation %])}])))


(defn start
  {:dev/after-load true}
  []
  (expo/render-root (reagent/as-element [root])))

(defn init []
  (start))
