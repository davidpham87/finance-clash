(ns finance-clash.quizz
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.set :refer (rename-keys)]
            [clojure.pprint :refer (pprint)]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [java-time :as jt]
            [finance-clash.db :refer (execute-query!)]
            [finance-clash.budget :as budget]
            [honeysql.core :as sql]
            [honeysql.helpers :as hsql
             :refer (select where from insert-into)]
            [muuntaja.core :as mc]
            [muuntaja.format.yaml :as yaml]
            [reitit.coercion.spec]
            [spec-tools.spec :as spec]))

;; Import data
;; This should be on its own namespace ideally.
(def mi
  (mc/create (-> mc/default-options
                 (mc/install yaml/format))))

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

(defn read-questions [chapter]
  (->> (get question-files chapter)
       (str "questions/") clojure.java.io/resource slurp
       (mc/decode mi "application/x-yaml")))

(defn convert-question [chapter]
  (let [new-filename (-> (get question-files chapter)
                         str/lower-case
                         (str/replace #"yaml$" "json")
                         (as-> s (clojure.core/str "assets/questions/" s)))
        data (->> (read-questions chapter)
                  (mc/encode mi "application/json")
                  slurp)]
    (println new-filename)
    (spit new-filename data)))

(defn format-question->db [m chapter number]
  #_(println m)
  (-> m
      (assoc :id (str chapter "_" number)
             :chapter chapter
             :number number)
      (dissoc :question :responses)
      (rename-keys {:correct-response :correct_response})))

(defn import-question->db [chapter]
  (let [data (->> (read-questions chapter)
                  (mc/encode mi "application/json")
                  slurp)
        data (json/read-str data :key-fn keyword)
        query (fn [data chapter]
                (-> (insert-into :questions)
                    (hsql/values (map-indexed #(format-question->db %2 chapter %1) data))
                    (sql/format)))]
    (execute-query! (query data chapter))))

;; REST API

(defn get-all-ids []
  (-> (select :id)
      (from :questions)
      (sql/format)
      (execute-query!)))

(defn init-chapters
  "Fill chapters table."
  []
  (-> {:insert-into :chapters
       :values (for [i (range 26)] {:chapter i})}
      (sql/format)
      (execute-query!)))

;; questions
(s/def ::chapter spec/int?)
(s/def ::question spec/int?)

(s/def ::user-id spec/string?)

(s/def ::selected-response spec/int?)
(s/def ::series spec/string?)
(s/def ::weight spec/int?)

(defn quizz-tx [question-id user-id series-id]
  (-> (select :success) (from :quizz_attempt)
      (where [:= :question question-id] [:= :user user-id]
             [:= :series series-id])
      (hsql/limit 1)
      (sql/format)
      (execute-query!)))

(defn correct-answer? [question-id user-id selected-answer]
  (let [correct-response
        (fn [id]
          (-> (select :correct_response)
              (from :questions)
              (hsql/where [:= :id id])
              (hsql/limit 1)
              (sql/format)
              (execute-query!)))]
    (= (-> (correct-response question-id) first :correct_response)
       selected-answer)))

(defn attempt! [question-id user-id series success?]
  (let [tx (quizz-tx question-id user-id series)
        update-query
        (-> (hsql/update :quizz_attempt)
            (hsql/sset
             {:success (or (pos? (-> tx first :success)) success?)
              :attempt (sql/call :+ :attempt (if (pos? (-> tx first :success)) 0 1))})
            (where [:= :question question-id] [:= :user user-id]
                   [:= :series series]))
        insert-query
        (-> (hsql/insert-into :quizz_attempt)
            (hsql/values [{:question question-id :user user-id :attempt 1
                           :series series
                           :success success?}]))
        query (if (seq tx) update-query insert-query)]
    #_(println query (seq tx) (-> tx first :success))
    (execute-query! (sql/format query))))

(def routes-answer
  ["/answer"
   {:post
    {:summary "Save an answer from the user and return if the selected answer was correct."
     :parameters {:body (s/keys :req-un [::user-id ::selected-answer ::series])}
     :handler
     (fn [m]
       (let [params (:parameters m)
             {:keys [user-id selected-answer series]} (:body params)
             {:keys [chapter question]} (:path params)
             id (str chapter "_" question)
             tx (quizz-tx id user-id series)
             success? (correct-answer? id user-id selected-answer)]
         #_(println user-id selected-answer id success? series)
         (attempt! id user-id series success?)
         ;; check if success was false, must provide a value to zero? or error
         ;; will be thrown
         (when (and success?
                    (-> tx first :success (or 1) zero?))
           #_(budget/earn-question-value user-id question-id))
         ;; TODO(dph): implement earn-question-value
         {:status 200
          :body {:is_correct_response success?}}))}}])

;; Series
;; Series are a set of question which can only be release after the release date.
(s/def ::availability spec/boolean?)
(s/def ::release-date spec/string?)
(s/def ::available (s/coll-of spec/integer? :min-count 1 :distinct true))
(s/def ::priority (s/coll-of spec/integer? :min-count 1 :distinct true))

(defn available?
  "Query availability of question given their ids (chapter_number)"
  ([]
   (let [available
         (-> (select :*) (from :chapters) (sql/format) execute-query!)]
     available))
  ([ids] (available? ids true))
  ([ids v]
   (let [v (if (nil? v) true v)
         reset-available?
         (-> (hsql/update :chapters) (hsql/sset {:available (not v)})
             (where [:not-in :series ids]) sql/format)
         update-query
         (-> (hsql/update :chapters) (hsql/sset {:available v})
             (where [:in :series ids]) sql/format)]
     (execute-query! reset-available?)
     (execute-query! update-query)
     (zipmap ids (repeat true)))))

(defn priority?
  "Query priority of question given their ids (chapter_number)"
  ([]
   (let [priority
         (-> (select :*) (from :chapters) (sql/format) execute-query!)]
     priority))
  ([ids] (priority? ids true))
  ([ids v]
   (let [v (if (nil? v) true v)
         reset-priority
         (-> (hsql/update :chapters) (hsql/sset {:priority (not v)})
             (where [:not-in :series ids]) sql/format)
         new-priority
         (-> (hsql/update :chapters) (hsql/sset {:priority v})
             (where [:in :series ids]) sql/format)]
     (execute-query! reset-priority)
     (execute-query! new-priority)
     (zipmap ids (repeat true)))))

(defn get-questions [chapters-ids cols]
  (-> {:select cols
       :from [:questions]
       :where [:in :chapter chapters-ids]}
      sql/format))

(defn latest-series []
  (-> {:select [:series]
       :from [:quizz_series]
       :order-by [[:release_date :desc]]
       :limit 1}
      sql/format))

;; TODO(dph): latest should returns all the questions? Yes, but later
(def routes-latest
  ["/latest"
   {:get {:summary "Retrieve the latest series identifier."
          :handler
          (fn [m] {:status 200 :body {:series (latest-series)}})}}])

(defn reorder-priority [ms priority-ids n]
  (let [priority-ids (into #{} priority-ids)]
    (-> (take n (filter #(priority-ids (:chapter %)) ms))
        vec
        (as-> qs (into qs (remove #(contains? (into #{} qs) %) ms))))))

(defn get-series-questions [available-ids priority-ids]
  (let [priority-filter (into #{} priority-ids)
        questions
        (->> (get-questions available-ids [:id :chapter :difficulty])
             execute-query!
             shuffle
             (group-by :difficulty)
             (reduce-kv #(assoc %1 %2
                                (->> (reorder-priority %3 priority-ids 5)
                                     (mapv :id))) {}))]
    questions))

(def routes-series
  ["/series"
   {:coercion reitit.coercion.spec/coercion}
   [""
    {:post {:summary "Create a new series"
            :parameters {:body (s/keys :req-un [::available ::priority])}
            :handler
            (fn [{{{:keys [available priority]} :body} :parameters}]
              (available? available true)
              (priority? priority true)
              (-> (hsql/insert-into :quizz_series)
                  (hsql/values [{:release_date [sql/call :datetime "now" "utc"]}])
                  sql/format
                  execute-query!)
              {:status 200 :body {:series 0}})}}]
   ["/:series/questions"
    {:parameters {:path (s/keys :req-un [::series])}
     :get {:summary "Get series details questions"
           :handler
           (fn [m]
             (let [available-ids (available?)
                   priority-ids (filter :priorty available-ids)
                   questions (get-series-questions (mapv :id available-ids)
                                                   (mapv :id priority-ids))]
               {:status 200 :body questions}))}}]
   routes-latest])

(def routes
  [routes-series
   ["/quizz/:chapter/:question"
    {:coercion reitit.coercion.spec/coercion
     :parameters {:path (s/keys :req-un [::chapter ::question])}}
    routes-answer]])

(comment


  (-> (client/get "http://localhost:3000/user/1") :body)
  (->> (client/get "http://localhost:3000/series/0/available")
       :body
       (mc/decode mi "application/json"))

  (-> (client/put
       "http://localhost:3000/series/0/available"
       {:content-type :json
        :body (json/write-str {:release-date "2018-01-01"})})
      :body
      (json/read-str :key-fn keyword))

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

  (doseq [chapter (range (count question-files))]
    (convert-question chapter))

  (doseq [chapter (range (count question-files))]
    (import-question->db chapter))

  (def chapter 0)
  (def questions (-> (import-question chapter) (json/read-str :key-fn keyword)))

  (let [{:keys [a b]} {:a 2 :b 2}]
    (println a b))
  #_(map-indexed (fn [i q] (format-question->db q chapter i)) questions)

  )
