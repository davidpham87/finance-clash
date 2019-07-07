(ns test.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 :drawer-open?
 (fn [db _] (get-in db [:ui-states :drawer-open?] false)))

(reg-sub
 :navigator
 (fn [db _] (:navigator db)))

(reg-sub
 :active-screen
 (fn [db _] (:active-screen db)))
