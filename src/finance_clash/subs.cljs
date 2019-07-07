(ns finance-clash.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 :navigator
 (fn [db _] (:navigator db)))

(reg-sub
 :navigator-state
 :<- [:navigator]
 (fn [navigator _]
   (-> (.-state navigator) (js->clj :keywordize-keys true))))

(reg-sub
 :active-screen
 (fn [db _] (:active-screen db)))
