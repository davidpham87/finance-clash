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
    [day8.re-frame.http-fx]
    [shadow.expo :as expo]
    [clojure.string :as str]
    [re-frisk-remote.core :refer [enable-re-frisk-remote!]]

    [finance-clash.specs.questions]

    [finance-clash.events]
    [finance-clash.subs]
    [finance-clash.portfolio.views :refer (home)]
    [finance-clash.questions.views]
    [finance-clash.views :refer [app-container]]
    [finance-clash.shared.bottom-nav :refer (active-screen)]))

;; must use defonce and must refresh full app so metro can fill these in
;; at live-reload time `require` does not exist and will cause errors
;; must use path relative to :output-dir

#_(enable-re-frisk-remote!
 {:enable-re-frisk? false
  :enable-re-frame-10x? true})

(defonce navigator (rf/subscribe [:navigator]))

(defn root []
  (let [navigator (rf/subscribe [:navigator])
        active-screen (rf/subscribe [:active-screen])]
    (fn []
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
  (expo/render-root (reagent/as-element [root]))
  (let [navigator (rf/subscribe [:navigator])
        active-screen (rf/subscribe [:active-screen])]
    (.log js/console
          "Navigator"
          @navigator
          "Activescreen"
          (str @active-screen))
    (when (and @navigator @active-screen)
      (rf/dispatch [:set-active-screen @active-screen]))))

