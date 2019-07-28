(ns finance-clash.questions.events
  (:require
   [re-frame.core :as rf :refer [reg-event-fx reg-event-db reg-fx]]
   [day8.re-frame.http-fx]
   [ajax.core :as ajax]
   ["react-navigation" :as rnav]))

(def fetch (.-fetch js/window))
(def require-js (.-require js/window))

(reg-fx
 :load-data
 (fn [db _]
   (.then (fetch "../assets/questions/1_Intro.yml")
          (fn [response]
            (let [text (.text response)]
              (.log js/console text)
              (rf/dispatch [::success-request-question-specs text])
              (.warn js/console (.stringify (.-JSON js/window) text)))))))

(reg-event-fx
 ::request-question-specs
 (fn [{:keys [db]} [_ lesson]]
   {:db db
    :load-data nil
    #_{:uri "../assets/questions/1_Intro.yml"
     :method :get
     :timeout 8000
     :response-format (ajax/text-response-format)
     :on-success [::success-request-question-specs]
     :on-failure [::failure-request-question-specs]}}))

(reg-event-fx
 ::success-request-question-specs
 (fn [{:keys [db]} [_ result]]
   {:db (assoc-in db [:questions 1] result)}))

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
