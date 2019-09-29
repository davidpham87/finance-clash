(ns finance-clash-web.chapter-selection.subs
  (:require [re-frame.core :as rf :refer (reg-sub)]))

(reg-sub
 ::chapter-available
 (fn [db _]
   (get-in db [:chapter-selection :available])))

(reg-sub
 ::chapter-priority
 (fn [db _]
   (get-in db [:chapter-selection :priority])))
