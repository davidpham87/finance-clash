(ns finance-clash.quiz
  (:require
   #_[datahike.api :as d]
   [clj-http.client :as client]
   [clojure.data.generators :as gen]
   [clojure.data.json :as json]
   [clojure.edn :refer (read-string)]
   [clojure.java.io]
   [clojure.pprint :refer (pprint)]
   [clojure.set :as set :refer (rename-keys)]
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [clojure.walk :refer (postwalk-replace)]
   [datomic.api :as d]
   [finance-clash.auth :refer (protected-interceptor)]
   [finance-clash.budget :as budget]
   [finance-clash.db]
   [java-time :as jt]
   [muuntaja.core :as mc]
   [muuntaja.format.yaml :as yaml]
   [reitit.coercion.spec]
   [spec-tools.spec :as spec]))

;; Import data
;; This should be on its own namespace ideally.
(def random-seed 1)

(def execute-query! (constantly true))

(def mi (mc/create (-> mc/default-options (mc/install yaml/format))))

(def question-files
  ["0_Key_Notions.yaml"
   "1_Intro.yaml"
   "2_Etats_Financiers.yaml"
   "3_Le_Bilan.yaml"
   "4_Le_Compte_de_Resultat.yaml"
   "5_Introduction_Finance.yaml"
   "6_Le_Capital.yaml"
   "7_Cycles_Exploitation_Inv_Financement.yaml"
   "8_Comptabilite.yaml"
   "9_SIG_et_CAF.yaml"
   "10_Obligations.yaml"
   "11_Regulatory_Requirements.yaml"
   "12_Financial_Crisis.yaml"
   "13_TVA.yaml"
   "14_Diagnostic_Financier.yaml"
   "15_Financial_Risks_and_Options.yaml"
   "16_Couts_and_Comptabilite_Analytique.yaml"
   "17_Microeconomie.yaml"
   "18_Central_Banks.yaml"
   "19_Tresorerie.yaml"
   "20_empty.yaml"
   "21_Rating_agencies.yaml"
   "22_Credit_Talk.yaml"
   "23_Libor_Fwd_Rates.yaml"
   "24_Bourse.yaml"
   "25_Liquidity_Talk.yaml"])

(defn read-questions [question-files chapter]
  (->> (get question-files chapter)
       (str "questions/") clojure.java.io/resource slurp
       (mc/decode mi "application/x-yaml")))

(defn convert-question [question-files chapter]
  (let [new-filename
        (-> (get question-files chapter)
            str/lower-case
            (str/replace #"yaml$" "json")
            (as-> s (clojure.core/str "assets/questions/" s)))
        data (->> (read-questions question-files chapter)
                  (mc/encode mi "application/json")
                  slurp)]
    (println new-filename)
    (spit new-filename data)))

(defn format-question->db [m chapter idx]
  (let [choices->answers
        (fn [choices]
          (vec (map-indexed
                (fn [i s] {:db/id (str (d/tempid :answer))
                           :answer/value (str s) :answer/position (inc i)}) choices)))
        questions
        (->>
         (set/rename m {:correct-response :question/answers
                        :question :question/question
                        :responses :question/choices
                        :duration :question/duration
                        :difficulty :question/difficulty})
         (into []
               (comp
                (map #(update % :question/difficulty
                              (fn [s] (if (seq s)
                                        (keyword "question.difficulty" s)
                                        (keyword "question.difficulty" :medium)))))
                (map #(update % :question/choices choices->answers))
                (map #(assoc % :question/title (get % :question/question)))
                (map #(update % :question/answers
                              (fn [i]
                                (-> (get % :question/choices)
                                    (nth (dec (int i))) :db/id))))
                (map #(assoc % :question/tags
                             {:tags/description "chapter"
                              :tags/value (str idx " " chapter)}))
                (map #(assoc % :db/id (str (d/tempid :chapter)))))))]
    {:quiz/title (str idx " " chapter)
     :quiz/questions questions}))

(defn import-question->db []
  (let [data (for [idx (range (count question-files))
                   :let [chapter (-> (nth question-files idx)
                                     (str/replace #".yaml" "")
                                     (str/split  #"_")
                                     rest
                                     (as-> $ (str/join " " $)))]]
               (format-question->db (read-questions question-files idx) chapter idx))]
    (d/transact (finance-clash.db/get-conn) (vec data))))

(defn update-question! [tx-data]
  (d/transact (finance-clash.db/get-conn) tx-data))

(defn questions [quiz-titles]
  (->> (d/q '[:find ?e
              :in $ [?qt ...]
              :where
              [?q :quiz/title ?qt]
              [?q :quiz/questions ?e]]
            (finance-clash.db/get-db)
            quiz-titles)
       (mapv first)))

(defn create-problems
  [{:problems/keys [title kind start deadline shuffle?]
    :keys [quizzes] :as m}]
  (let [questions quizzes
        data (->> '[title kind start deadline shuffle?]
                  (map str)
                  (map (partial keyword "problems"))
                  (select-keys m))]
    (->> (assoc data :problems/questions questions)
         vector
         (d/transact (finance-clash.db/get-conn)))))

;; REST API

(defn get-all-ids []
  (d/q '[:find ?e
         :where
         [?e :question/title]]
       (finance-clash.db/get-db)))

(comment
  (get-all-ids))

;; questions
(s/def ::chapter spec/int?)
(s/def ::question spec/int?)

(s/def ::user-id spec/string?)

(s/def ::selected-response spec/int?)
(s/def ::series (s/or ::int spec/int? ::str spec/string?))
(s/def ::weight spec/int?)
(s/def ::tx-data spec/coll?)

(s/def ::deadlines spec/string?)
(s/def ::quizzes spec/coll?)

(defn quiz-tx [problem-id question-id user-id ]
  (let [db (finance-clash.db/get-db)]
    (->> (d/q '[:find (pull ?e [*])
                :in $ ?q ?u ?p
                :where
                [?p :problems/transactions ?e]
                [?e :transaction/question ?q]
                [?e :transaction/user ?u]]
              db
              question-id
              [:user/id user-id]
              problem-id)
         (mapv first))))

(defn problems->question-id [series-id question-title]
  (->> (d/q '[:find [?qid]
              :in $ ?s ?q
              :where
              [?p :problems/title ?s]
              [?p :problems/questions ?qid]
              [?qid :question/title ?q]]
            (finance-clash.db/get-db)
            series-id
            question-title)
       first))

(defn correct-answer? [question-id answer-position]
  (let [correct-responses
        (->> (d/pull (finance-clash.db/get-db)
                     '[{:question/answers [:answer/position]}]
                    question-id)
             :question/answers
             (into #{} (map :answer/position)))]
    (contains? correct-responses answer-position)))

(defn attempt! [series-id question-id user-id user-answer success?]
  (let [tx-data [{:db/id (str -1)
                  :transaction/user [:user/id user-id]
                  :transaction/question question-id
                  :transaction/answer (str user-answer)
                  :transaction/correct? success?}
                 {:db/id series-id
                  :problems/transactions [(str -1)]}]]
    (d/transact (finance-clash.db/get-conn) tx-data)))

(def routes-answer
  ["/answer"
   {:post
    {:summary "Save an answer from the user and return if the selected answer was correct."
     :parameters {:body (s/keys :req-un [::user-id ::selected-response ::series ::question])}
     :interceptor [protected-interceptor]
     :handler
     (fn [m]
       (let [params (:parameters m)
             {:keys [user-id selected-response series question]} (:body params)
             tx (quiz-tx series question user-id)
             success? (correct-answer? question selected-response)]
         (attempt! series question user-id selected-response success?)
         (pprint tx)
         (pprint (and success?
                    (or (empty? tx)
                        (not-any? true? (map :transaction/correct? tx)))))
         (when (and success?
                    (or (empty? tx)
                        (not-any? true? (map :transaction/correct? tx))))
           (budget/earn-question-value! user-id question))
         {:status 200
          :body {:question/db-id question
                 :problems/db-id series
                 :answer-status (if success? "correct" "wrong")}}))}}])


;; Series
;; Series are a set of question which can only be release after the release date.
(s/def ::availability spec/boolean?)
(s/def ::release-date spec/string?)
(s/def ::available (s/coll-of spec/integer? :min-count 1 :distinct true))
(s/def ::priority (s/coll-of spec/integer? :min-count 1 :distinct true))

(defn get-questions [series]
  (d/q '[:find (pull ?q [*])
         :in $ ?p
         :where
         [?e :problems/title ?p]
         [?e :problems/questions ?q]]
       (finance-clash.db/get-db)
       series))

(comment (get-questions "First"))

(defn available-series []
  (->> (d/q '[:find ?e
              :in $ ?now
              :where
              [?e :problems/deadline ?d]
              [(< ?now ?d)]]
            (finance-clash.db/get-db)
            (java.util.Date.))
       (mapv first)))

(defn latest-series []
  (->>
   (available-series)
   (d/pull-many (finance-clash.db/get-db) [:db/id :problems/deadline])
   (sort-by :problems/deadline)
   first
   :db/id
   (d/pull (finance-clash.db/get-db) '[*])))

(defn available?
  "Query availability of question given their ids (chapter_number)"
  ([]
   (d/pull-many (finance-clash.db/get-db) [:db/id :problems/questions]
                (available-series)))
  ([ids] (available? ids true))
  ([ids v] (available?)))

(comment
  (let [deadline "2020-09-05"]
    (-> (.. (jt/local-date-time "yyyy-MM-dd HH:mm"
                                (str deadline " 10:00"))
            (atZone (jt/zone-id "Europe/Paris"))
            toInstant)
        java.util.Date/from
        )))

(defn parse-iso-date-string [s]
  (-> (.. (jt/local-date-time "yyyy-MM-dd HH:mm" s)
          (atZone (jt/zone-id "Europe/Paris"))
          toInstant)
      java.util.Date/from))


(def routes-series
  ["/series"
   {:coercion reitit.coercion.spec/coercion}
   [""
    {:post {:summary "Create a new series"
            :parameters {:body (s/keys :req-un [::deadline ::quizzes])}
            :interceptor [protected-interceptor]
            :handler
            (fn [m]
              {:status 200 :body "Safe"}
              (let [token-id (:identity m)
                    {:keys [deadline quizzes]} (get-in m [:parameters :body])
                    quizzes (postwalk-replace {:id :db/id} quizzes)
                    m (assoc #:problems{:title "Finance Clash"
                                        :kind :homework
                                        :start (java.util.Date.)
                                        :deadline (parse-iso-date-string deadline)
                                        :shuffle? true}
                             :quizzes quizzes)]
                (if (#{{:user "admin"} {:user "neo2551"}} token-id)
                  (do
                    (let [db-data (create-problems m)]
                      (println db-data))
                    {:status 200 :body {:message "Series created"}})
                  {:status 403 :body {:message "Unautorized"}})))}}]
   ["/:series/questions"
    {:parameters {:path (s/keys :req-un [::series])}
     :get {:summary "Get series details questions"
           :handler
           (fn [m] (let [questions (available?)] {:status 200 :body questions}))}}]
   ["/latest"
    {:get {:summary "Retrieve the latest series identifier."
           :handler
           (fn [m] {:status 200 :body {:series (latest-series)}})}}]])

(defn get-chapters []
  (d/q '[:find (pull ?e [*])
         :where
         [?e :quiz/title]]
       (finance-clash.db/get-db)))

(def routes
  [routes-series
   ["/quiz"
    {:coercion reitit.coercion.spec/coercion}
    routes-answer]
   ["/quiz/chapters"
    {:get {:summary "Return the chapters table"
           :handler (fn [m] {:status 200 :body (mapv first (get-chapters))})}
     :post {:summary "Return the chapters table"
            :coercion reitit.coercion.spec/coercion
            :interceptor [protected-interceptor]
            :parameters {:body (s/keys :req-un [::tx-data])}
            :handler
            (fn [m]
              (let [token-id (:identity m)
                    {:keys [tx-data]} (get-in m [:parameters :body])
                    tx-data (postwalk-replace
                             {:value :answer/value :id :db/id :position :answer/position
                              :answers :question/answers
                              :retract :db/retract
                              "retract" :db/retract
                              "answers" :question/answers}
                             tx-data)]
                (if (#{{:user "admin"} {:user "neo2551"}} token-id)
                  (do
                    (update-question! tx-data)
                    {:status 200 :body {:message "Saved result"}})
                  {:status 403 :body {:message "Unauthorized"}})))}}]])


(comment
  (-> (read-questions questions-files 0)
      (set/rename {:correct-response :question/answer
                   :question :question/title
                   :responses :question/choices
                   :duration :question/duration
                   :difficulty :question/difficulty})
      (as-> $
          (map #(update % :question/difficulty
                        (fn [s] (keyword "question.difficulty" s))) $))))

(comment

  (import-question->db)

  (questions ["Key Notions" "Libor Fwd Rates"])
  (d/pull-many (finance-clash.db/get-db) '[*]
               (questions ["Key Notions" "Libor Fwd Rates"]))

  (->> (d/q {:find '[?q]
            :in '[$ [[?c ?t] ...]]
            :where
            '[[?e :quiz/title ?c]
              [?e :quiz/questions ?q]
              [?q :question/title ?t]]}
           (finance-clash.db/get-db)
           [["Key Notions" "Quelle affirmation est correcte?"]
            ["Key Notions" "Pour le calcul d'une valeur actualisée, le Discount Factor?"]])
       (mapv first)
       (d/pull-many (finance-clash.db/get-db) '[*]))

  (d/pull (finance-clash.db/get-db) '[*] 17592186045428)
  (d/pull (finance-clash.db/get-db) '[*] 59)
  (d/datoms (finance-clash.db/get-db) :eavt)

  (let [m (assoc #:problems{:title "First"
                            :kind :homework
                            :start #inst "2020-01-01"
                            :deadline #inst "2020-12-31"
                            :shuffle? false}
                 :quizzes (questions ["Key Notions" "Libor Fwd Rates"]))]
    (create-problems m))

  (d/q '[:find (pull ?e [*])
         :where
         [?e :problems/title "First"]]
       (finance-clash.db/get-db))

  (d/transact (finance-clash.db/get-conn)
              [[:db/retractEntity 17592186045426]])

  (import-question->db)

  (->> (d/q '[:find ?t
              :where
              [?e :quiz/title ?t]]
            (finance-clash.db/get-db))
       (map first)
       #_(d/pull (finance-clash.db/get-db) '[*]))

  (->> (d/q '[:find (pull ?e [*])
              :where
              ;; [?q :quiz/title "Bourse"]
              ;; [?q :quiz/questions ?e]
              [?e :question/difficulty :question.difficulty/empty]]
            (finance-clash.db/get-db))))

(comment
  (d/pull (finance-clash.db/get-db)
          '[*]
          17592186046433)

  (d/q '[:find (pull ?q [*])
         :where
         [?e :quiz/title "6 Le Capital"]
         [?e :quiz/questions ?q]]
       (finance-clash.db/get-db))

  [(correct-answer? 17592186046433 4)
   (correct-answer? 17592186046433 3)
   (correct-answer? 17592186046433 2)
   (correct-answer? 17592186046433 1)]

  (d/q '[:find ?s
         :in $
         :where
         [?p :problems/title ?s]

         ;; [?qid :question/title ?q]
         ]
       (finance-clash.db/get-db)
       )

  ;; update with correct answer
  #_(quiz-tx "The 'no arbitrage' principle in simple terms is?" "neo" "First")
  (problems->question-id "First" "The 'no arbitrage' principle in simple terms is?")
  (d/pull (finance-clash.db/get-db) '[*] 17592186046487)

  (attempt! "First" "The 'no arbitrage' principle in simple terms is?" "neo" "World" true)

  (-> (d/q '[:find [?e]
             :in $ ?t
             :where [?e :problems/title ?t]]
           (finance-clash.db/get-db)
           "First")
      first)

  (d/pull (finance-clash.db/get-db) '[*] 17592186046543))

(comment
  (-> (client/get "http://localhost:3000/user/1") :body)
  (->> (client/get "http://localhost:3000/series/latest/questions")
       :body
       (mc/decode mi "application/json"))

  (->> (client/post "http://localhost:3000/series"
                    {:content-type :json
                     :headers {:Authorization "Token eyJhbGciOiJIUzUxMiJ9.eyJ1c2VyIjoibmVvIn0.7ylkQaztX6BEa8iHBExYduVXbs4H3pbtSP3Z2FK2gmfRG9GDppHx5d9JgDO6GpZjhbQcd8RBRn6M_FXIW1Qxlw"}
                     :body (json/write-str
                            {:deadline "2020-09-28T09:00:00.000Z"
                             :quizzes [[{"id" 17592186046491}
                                        {"id" 17592186046168}
                                        {"id" 17592186046435}
                                        {"id" 17592186046014}
                                        {"id" 17592186045481}
                                        {"id" 17592186045641}]]})}
                    )
       :body
       (mc/decode mi "application/json"))

  (-> (client/put
       "http://localhost:3000/series/0/available"
       {:content-type :json
        :body (json/write-str {:release-date "2018-01-01"})})
      :body
      (json/read-str :key-fn keyword))

  (-> (client/post
       "http://localhost:3000/quiz/answer"
       {:content-type :json
        :headers {:Authorization "Token eyJhbGciOiJIUzUxMiJ9.eyJ1c2VyIjoibmVvIn0.7ylkQaztX6BEa8iHBExYduVXbs4H3pbtSP3Z2FK2gmfRG9GDppHx5d9JgDO6GpZjhbQcd8RBRn6M_FXIW1Qxlw"}
        :body (json/write-str
               {:question 17592186045455
                :series 17592186046559
                :selected-response 2
                :user-id "neo"})})
      :body
      (json/read-str :key-fn keyword))

  (quiz-tx 17592186046581 17592186045475 "neo")
  (attempt! 17592186046559 17592186045455 "neo" 2 true)
  (correct-answer? 17592186045455 2)

  (budget/earn-question-value! series question user-id)
  (budget/earn-question-value! "neo" 17592186045457)
  (budget/question-id->question-value 17592186045457)

  (->> 17592186045457
       (d/pull (finance-clash.db/get-db) [:question/difficulty])
       :question/difficulty
       :db/ident)

  (-> (client/post
       "http://localhost:3000/series/1/available"
       {:content-type :json
        :body (json/write-str {:release-date "2018-01-01"})})
      :body
      (json/read-str :key-fn keyword))

  (-> (client/post
       "http://localhost:3000/series"
       {:content-type :json
        :body (json/write-str
               {:available [1 2 3 4 5 6 7 8 9 10]
                :priority [8 9 10]})})
      :body
      (json/read-str :key-fn keyword))

  (-> (client/post
       "http://localhost:3000/question/0/1/answer"
       {:content-type :json
        :body (json/write-str {:user-id "1" :selected-answer 2})})
      :body
      (json/read-str :key-fn keyword))

  (-> (client/get "http://localhost:3000/echo") :body)
  (-> (client/get "http://localhost:3000/quiz/chapters") :body
      (json/read-str :key-fn keyword))

  (doseq [chapter (range (count question-files))]
    (convert-question question-files chapter))

  (doseq [chapter (range (count question-files))]
    (import-question->db chapter))

  (d/q '[:find (pull ?e [:quiz/title :db/id])
         :where
         [?e :quiz/title]]
       (finance-clash.db/get-db))

  (d/q '[:find (pull ?e [*])
         :where
         [?e :problems/deadline]]
       (finance-clash.db/get-db))

  (available?)

  (d/pull (finance-clash.db/get-db) '[*] 17592186045684)
  (d/transact (finance-clash.db/get-conn) [[:db/retract 17592186045684
                                          :question/answers 17592186045688]])
  (get-chapters)
  (def chapter 0)
  (def questions (-> (import-question chapter) (json/read-str :key-fn keyword)))



  (let [{:keys [a b]} {:a 2 :b 2}]
    (println a b))
  #_(map-indexed (fn [i q] (format-question->db q chapter i)) questions)
  (parse-iso-date-string "2020-09-28 09:00")
)
