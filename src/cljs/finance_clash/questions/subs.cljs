(ns finance-clash.questions.subs
  (:require
   [re-frame.core :as rf :refer [reg-sub]]))

(reg-sub
 ::questions
 (fn [db [_ chapter]]
   (get-in db [:questions chapter])))

