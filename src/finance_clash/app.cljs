(ns finance-clash.app
  (:require
    ["expo" :as ex]
    ["react-native" :as rn]
    ["react" :as react]
    ["react-navigation" :as rnav]
    ["react-native-screens" :refer (useScreens)]

    [goog.object :as gobj]
    [reagent.core :as reagent]
    [re-frame.core :as rf]
    [shadow.expo :as expo]
    [clojure.string :as str]
    [re-frisk-remote.core :refer [enable-re-frisk-remote!]]

    [finance-clash.specs.questions]

    [finance-clash.events]
    [finance-clash.subs]
    [finance-clash.portfolio.views :refer (home)]
    [finance-clash.questions.views]
    [finance-clash.views :refer [app-container]]))

;; must use defonce and must refresh full app so metro can fill these in
;; at live-reload time `require` does not exist and will cause errors
;; must use path relative to :output-dir

(enable-re-frisk-remote!
 {:enable-re-frisk? false
  :enable-re-frame-10x? true})

(def navigator (rf/subscribe [:navigator]))

(defn root []
  (let [navigator (rf/subscribe [:navigator])
        active-screen (rf/subscribe [:active-screen])]
    (fn []
      (when (and @navigator @active-screen)
        (rf/dispatch [:set-active-screen @active-screen]))
      [:> app-container {:ref #(rf/dispatch [:set-navigation %])}])))

(defn start
  []
  (gobj/set js/console "disableYellowBox" true)
  (useScreens)
  (rf/dispatch-sync [:initialize-db])
  (expo/render-root (reagent/as-element [root])))

(defn init {:dev/after-load true} []
  (gobj/set js/console "disableYellowBox" true)
  (.log js/console "Reload")
  (expo/render-root (reagent/as-element [root])))

