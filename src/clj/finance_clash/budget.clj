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
(def question-value-raw {:easy 10 :medium 21 :hard 30})
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

(defn buy [user-id v]
  (budget-tx user-id (- v))
  (budget user-id (- v)))

(defn earn [user-id v]
  (budget-tx user-id v)
  (budget user-id v))

(defn question-value
  [difficulty {:keys [priority? bonus-period?] :as modifiers}]
  (* (get question-value (keyword difficulty) 0)
     (question-bonus priority? 1)
     (if priority? (question-bonus :priority) 1)
     (if bonus-period? (question-bonus :bonus-period) (question-bonus :malus-period))))

(def routes-buy-question
  ["/quizz/buy-question"
   {:coercion reitit.coercion.spec/coercion
    :parameters {:body (s/keys :req-un [::user-id ::diffculty])}
    :post {:handler
           (fn [{{{:keys [user-id difficulty]} :body} :parameters}]
             (let [x (question-price (keyword difficulty))] (buy user-id x)))}}])

(def routes routes-buy-question)

(comment
  (budget "1")
  (wealth "1")
  (earn "1" 500)
  (buy "1" 200)
  (budget-tx "1"))
