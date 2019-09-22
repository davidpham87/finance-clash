(ns finance-clash-web.quizz.subs
  (:require
   [goog.object :as gobj]
   [clojure.string :as s]
   [re-frame.core :as rf :refer (reg-sub)]))

(reg-sub
 ::question
 (fn [db [_ id]]
   (let [[chapter question] (s/split id #"_")
         question (js/parseInt question)]
     (println chapter)
     (println question)
     (println (nth (get-in db [:question-data chapter]) question))
     (get-in db [:question-data chapter question]))))
