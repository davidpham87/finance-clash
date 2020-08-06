(ns finance-clash-web.core
  (:require
   [finance-clash-web.events]
   [finance-clash-web.subs]
   [finance-clash-web.views :refer (app)]
   [goog.events :as events]
   [re-frame.core :as rf]
   [reagent.dom :as dom]))

(defn window-event-listeners []
  (.addEventListener
   js/window events/EventType.RESIZE
   #(rf/dispatch [:record-window-size (.-innerWidth js/window)
                  (.-innerHeight js/window)])))

(defn mount-app []
  (dom/render [app] (.getElementById js/document "app")))

(defn ^:export main []
  (window-event-listeners)
  (rf/dispatch [:initialise-db])
  (mount-app))

(defn ^:dev/after-load start []
  (.log js/console "Hello I know it")
  (mount-app))


(comment
  (mount-app)
  )
