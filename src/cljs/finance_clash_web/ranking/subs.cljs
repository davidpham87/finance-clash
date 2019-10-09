(ns finance-clash-web.ranking.subs
  (:require [re-frame.core :refer (reg-sub)]))

(reg-sub
 ::ranking
 (fn [db _] (:ranking db)))
