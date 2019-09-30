(ns finance-clash.budget
  "Implements budget tables and routes"
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.set :refer (rename-keys)]
            [clojure.pprint :refer (pprint)]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [java-time :as jt]
            [finance-clash.db :refer (execute-query!)]
            [honeysql.core :as sql]
            [honeysql.helpers :as hsql
             :refer (select where from insert-into)]
            [muuntaja.core :as mc]
            [muuntaja.format.yaml :as yaml]
            [reitit.coercion.spec]
            [spec-tools.spec :as spec]))

(def question-price {:easy 3 :medium 7 :hard 10})
(def question-value-raw {:easy 12 :medium 28 :hard 40})
(def question-bonus {:priority? 1.2 :bonus-period? 1.2 :malus-period? 0.5})

(s/def ::user-id spec/string?)
(s/def ::diffculty spec/string?)

(defn budget-tx
  "budget transaction query"
  ([user-id]
   (-> {:select [:*] :from [:budget_history] :where [:= :user user-id]}
       sql/format
       execute-query!))
  ([user-id value]
   (-> {:insert-into :budget_history
        :values [{:user user-id :exchange_value value
                  :update_at (sql/call :datetime "now" "utc")}]}
       sql/format
       execute-query!)))

(defn clear-budget-tx [user-id]
  (-> {:delete-from  :budget_history
       :where [:= :user user-id]}
      sql/format
      execute-query!))

(defn budget-init [user-id v]
  (-> {:insert-into :budget
       :values [{:user user-id :wealth (or v 100)}]}
      sql/format
      execute-query!))

(defn budget-reset [user-id v]
  (-> (hsql/update :budget)
      (hsql/sset {:wealth v
                  :update_at (sql/call :datetime "now" "utc")})
      (where [:= :user user-id])
      sql/format
      execute-query!))

(defn budget
  ([user-id]
   (-> {:select [:*] :from [:budget] :where [:= :user user-id] :limit 1}
       sql/format
       execute-query!))
  ([user-id v]
   (-> (hsql/update :budget)
       (hsql/sset {:wealth (sql/call :+ :wealth v)
                   :update_at (sql/call :datetime "now" "utc")})
       (where [:= :user user-id])
       sql/format
       execute-query!)))

(defn wealth [user-id] (budget user-id))

(defn buy! [user-id v]
  (budget-tx user-id (- v))
  (budget user-id (- v)))

(defn earn! [user-id v]
  (budget-tx user-id v)
  (budget user-id v))

;; Compute bonus

(defn now []
  (jt/with-clock (jt/system-clock "Europe/Paris")
    (jt/offset-time)))

(defn bonus-period?
  "Bonus period is between 19 and 8 EU time."
  [h] (or (< h 8) (> h 19)))

(defn query-question [id]
  (-> {:select [:difficulty]
      :from [:questions]
      :where [:= :id id]
       :limit 1}
      sql/format
      execute-query!))

(defn chapter-priority [id]
  (-> {:select [:priority]
       :from [:chapters]
       :where [:= :chapter id]
       :limit 1}
      sql/format
      execute-query!))

(defn question-value
  [difficulty {:keys [priority? bonus-period?] :as modifiers}]
  (println difficulty modifiers)
  (* (get question-value-raw (keyword difficulty) 0)
     (if priority? (:priority? question-bonus) 1)
     (if bonus-period?
       (:bonus-period? question-bonus)
       (:malus-period? question-bonus))))

(defn question-id->question-value
  [question-id]
  (let [answer-hour (-> (now) .getHour)
        difficulty (-> (query-question question-id) first :difficulty keyword)
        chapter (first (clojure.string/split question-id #"_"))
        priority? (-> (chapter-priority chapter) first (:priority 0) pos?)]
    (question-value difficulty
                    {:priority? priority?
                     :bonus-period? (bonus-period? answer-hour)})))

(defn earn-question-value! [user-id question-id]
  (let [value (question-id->question-value question-id)]
    (earn! user-id value)))

;; Routes

(def routes-buy-question
  ["/quiz/buy-question"
   {:coercion reitit.coercion.spec/coercion
    :parameters {:body (s/keys :req-un [::user-id ::diffculty])}
    :post {:handler
           (fn [{{{:keys [user-id difficulty]} :body} :parameters}]
             (let [x (question-price (keyword difficulty))] (buy! user-id x)))}}])

(def routes routes-buy-question)

(comment
  (budget "1")
  (wealth "1")
  (earn "1" 500)
  (buy "1" 200)
  (budget-tx "1")
  (question-id->question-value "0_0")
  )
