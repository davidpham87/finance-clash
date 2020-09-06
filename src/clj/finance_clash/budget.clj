(ns finance-clash.budget
  "Implements budget tables and routes"
  (:require
   [clojure.edn]
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [datomic.api :as d]
   [finance-clash.auth :refer (protected-interceptor)]
   [finance-clash.db]
   [honeysql.core :as sql]
   [honeysql.helpers :as hsql
    :refer (select where from insert-into)]
   [java-time :as jt]
   [reitit.coercion.spec]
   [spec-tools.spec :as spec]))

(defn execute-query! [& rest])

(def question-price {:easy 5 :medium 12 :hard 17})
(def question-value-raw {:easy 12 :medium 28 :hard 40})
(def question-bonus {:priority? 1.2 :bonus-period? 1.2 :malus-period? 0.5})

(s/def ::user-id spec/string?)
(s/def ::difficulty spec/string?)

(defn budget-tx
  ([user-id]
   (d/pull (finance-clash.db/get-db) [:user/transactions] [:user/id user-id]))
  ([user-id {:keys [value reason]}]
   (let [tx-data #:user{:id user-id :transactions
                        #:user.transactions{:amount value :reason reason}}
         tx-data (cond-> tx-data
                   (nil? reason) (update-in [:user/transactions]
                                            dissoc :user.transactions/reason))]
     (d/transact (finance-clash.db/get-conn) [tx-data]))))

(defn clear-budget-tx! [user-id]
  (let [db (finance-clash.db/get-db)
        eids (d/pull db [{:user/transactions [:db/id]}] user-id)
        tx-data (mapv #(vector :db/retractEntity %) eids)]
    (d/transact (finance-clash.db/get-conn) tx-data)))

(defn budget-init [user-id v]
  (clear-budget-tx! user-id)
  (budget-tx user-id {:value v :reason "init"}))

(defn budget-init-all [v]
  (let [users (d/q '[:find ?u
                    :where
                    [_ :user/id ?u]]
                   (finance-clash.db/get-db))
        tx-data (mapv (fn [u] #:user{:id u :transactions
                                     #:user.transaction{:amount v :reason "init"}})
                      users)]
    (d/transact (finance-clash.db/get-conn) [tx-data])))

(defn budget
  ([user-id]
   (transduce (map :user.transactions/amount) + 0 (:user/transactions (budget-tx user-id))))
  ([user-id v]
   (let [user-budget (budget user-id)]
     (budget-tx user-id {:value (- v user-budget)}))))

(defn wealth [user-id] (budget user-id))

(defn buy! [user-id v]
  (budget-tx user-id {:value (- v) :reason "buy"}))

(defn earn! [user-id v]
  (budget-tx user-id {:value v :reason "earn"}))

;; Compute bonus
(defn now []
  (jt/with-clock (jt/system-clock "Europe/Paris")
    (jt/offset-date-time)))

(defn date->offset-datetime [date]
  (let [s (str (.toInstant date))
        years (subs s 0 4)
        month (subs s 5 7)
        days(subs s 8 10)
        hours (subs s 11 13)
        minutes (subs s 14 16)]
    (jt/with-clock (jt/system-clock "Europe/Paris")
      (->> [years month days hours minutes]
           (mapv clojure.edn/read-string)
           (apply jt/offset-date-time)))))


(defn bonus-period?
  "Bonus period is between 19 and 9 EU time. [Depcrecated]"
  [h] #_(or (< h 9) (> h 18))
  false)

(defn problems->question-id [series-id question-title]
  (let [db (finance-clash.db/get-db)]
    (->> (d/q '[:find [?qid]
                :in $ ?s ?q
                :where
                [?p :problems/title ?s]
                [?p :problems/questions ?qid]
                [?qid :question/title ?q]]
              db
              series-id
              question-title)
         first)))

(defn chapter-priority [id]
  (-> {:select [:priority]
       :from [:chapters]
       :where [:= :chapter id]
       :limit 1}
      sql/format
      execute-query!))

(defn ranking
  ([] (ranking 30))
  ([limit]
   (-> {:select [:username [(sql/call :round :wealth 2) :wealth]]
        :from [:budget]
        :left-join [:user [:= :user.id :budget.user]]
        :order-by [[:wealth :desc]]
        :limit limit}
       sql/format
       execute-query!)))

(defn question-value [difficulty] (get question-value-raw (keyword difficulty) 0))

(defn question-id->question-value
  [problem-title question-title]
  (let [difficulty (->> (problems->question-id problem-title question-title)
                        (d/pull (finance-clash.db/get-db) [:question/difficulty])
                        :question/difficulty
                        keyword)]
    (question-value difficulty)))

(defn earn-question-value! [user-id problem-title question-id]
  (let [value (question-id->question-value problem-title question-id)]
    (earn! user-id value)))

;; Routes
(def routes-buy-question
  o[["/quiz/buy-question"
    {:coercion reitit.coercion.spec/coercion
     :post
     {:interceptors [protected-interceptor]
      :parameters {:body (s/keys :req-un [::user-id ::difficulty])}
      :handler
      (fn [m]
        (let [{{{:keys [user-id difficulty]} :body} :parameters} m
              x (question-price (keyword difficulty))]
          (buy! user-id x)
          {:status 200 :body
           {:status :successful-transaction
            :cost x
            :message (str "Acquired question for " x "€.")}}))}}]
   ["/ranking"
    {:coercion reitit.coercion.spec/coercion
     :get
     {:handler (fn [m] {:status 200 :body (ranking)})}}]])

(def routes routes-buy-question)

(comment
  (budget "1")
  (wealth "1")
  (earn "1" 500)
  (buy "1" 200)
  (budget-tx "1")
  (question-id->question-value "0_0")
  (budget-tx "neo2551" {:value 100 :reason "Initial"})
  (transduce (map :user.transactions/amount) + 0 (:user/transactions (budget-tx "neo2551")))
  (budget "neo2551")
  (buy! "neo2551" 100)
  (earn! "neo2551" 300)

  )
