(ns finance-clash.subs
  (:require [re-frame.core :refer [reg-sub]]
            [finance-clash.shared.bottom-nav :refer (active-screen)]))

(reg-sub
 :navigator
 (fn [db _] (:navigator db)))

(reg-sub
 :navigator-state
 :<- [:navigator]
 (fn [navigator [_ ->clj?]]
   (cond-> (.-state navigator)
       ->clj?  (js->clj :keywordize-keys true))))

(reg-sub
 :active-screen
 (fn [db]
   (:active-screen db)))


