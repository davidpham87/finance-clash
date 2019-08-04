(ns finance-clash.questions.events
  (:require
   [re-frame.core :as rf :refer [reg-event-fx reg-event-db reg-fx]]
   ;; [day8.re-frame.http-fx]
   ;; [ajax.core :as ajax]
   ["react-navigation" :as rnav]))

(def fetch (.-fetch js/window))
(def require-js (.-require js/window))

(reg-fx
 :load-data
 (fn [chapter]
   (.then
    (fetch (str "http://localhost:8050/question?chapter=" chapter))
    (fn [response]
      (let [text (.text response)]
        (.then
         text
         #(rf/dispatch
           [::success-request-question-specs
            chapter (js->clj (.parse (.-JSON js/window) %)
                             :keywordize-keys true)])))))))

(reg-event-fx
 ::request-question-specs
 (fn [{:keys [db]} [_ chapter]]
   {:db db
    :load-data (or chapter 0)}))

(reg-event-fx
 ::success-request-question-specs
 (fn [{:keys [db]} [_ chapter result]]
   {:db (assoc-in db [:questions chapter] result)}))

(reg-event-fx
 ::failure-request-question-specs
 (fn [{:keys [db]} [_ _ error]]
   {:db (assoc db [:errors] error)}))

(comment
  (rf/dispatch [::request-question-specs 1])
  (defonce question (js/require "../assets/questions/1_Intro.yaml"))
  (-> @re-frame.db/app-db :questions)
  (-> (fetch "http://192.168.0.26:19006/questions/1_Intro.yml")
      (.then (fn [response] (.text response)))
      (.then (fn [text] (.log js/console text))))
  (require-js "./questions/1_Intro.yml")
  (def g (-> @re-frame.db/app-db :questions (get 1))))
