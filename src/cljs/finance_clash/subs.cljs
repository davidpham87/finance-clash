(ns finance-clash.subs
  (:require [re-frame.core :as rf :refer [reg-sub]]
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

(reg-sub
 :window-dimensions
 (fn [db _]
   (:window-dimensions db)))

;; Simulate the breakpoint from material design
(reg-sub
 :window-dimensions
 :<- [:window-size]
 (fn [{:keys [width height]} _]
   (cond
     (>= width 1920) :xl
     (>= width 1280) :lg
     (>= width 960) :md
     (>= width 600) :sm
     :else :xs)))

(reg-sub
 :platform
 (fn [db _]
   (get-in db [:expo :platform])))
