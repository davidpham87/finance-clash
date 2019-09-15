(ns finance-clash.quizz
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.set :refer (rename-keys)]
            [clojure.pprint :refer (pprint)]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [finance-clash.db :refer (execute-query!)]
            [honeysql.core :as sql]
            [honeysql.helpers :as hsql
             :refer (select where from insert-into)]
            [muuntaja.core :as mc]
            [muuntaja.format.yaml :as yaml]
            [reitit.coercion.spec]
            [spec-tools.spec :as spec]))

(def mi
  (mc/create (-> mc/default-options
                 (mc/install yaml/format))))

(def question-files
  ["0_Key_Notions.yaml"
   "1_Intro.yaml"
   "2_Etats_Financiers.yaml"
   "3_Le_Bilan.yaml"])

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
  (println m)
  (-> m
      (assoc :id (str chapter "_" number)
             :chapter chapter
             :number number
             :availability true)
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

(defn availability
  "Query availability of question given their ids (chapter_number)"
  ([ids]
   (let [availabilities
         (-> (select :availability) (from :questions) (hsql/where [:in :id ids])
             (sql/format) execute-query!)]
     (zipmap ids availabilities)))
  ([ids v]
   (let [availabilities
         (-> (hsql/update :questions) (hsql/sset {:availability v})
             (hsql/where [:in :id ids]) (sql/format) execute-query!)]
     (zipmap ids availabilities))))

(defn questions [])

(defn get-all-ids []
  (-> (select :id)
      (from :questions)
      (sql/format)
      (execute-query!)))


(s/def ::chapter spec/int?)
(s/def ::question spec/int?)


(s/def ::availability spec/boolean?)
(def routes-availability
  ["/available"
   {:get {:summary "Retrieve questions availability for a given chapter and number."
          :handler
          (fn [{{m :path} :parameters}]
            (let [id (str (:chapter m) "_" (:question m))]
              {:status 200
               :body (merge m (get (availability [id]) id))}))}
    :put {:summary "Set questions availability for a given chapter and number."
          :parameters {:body (s/keys :req-un [::availability])}
          :handler
          (fn [m]
            (let [v (get-in m [:parameters :body :availability])
                  m (get-in m [:parameters :path])
                  id (str (:chapter m) "_" (:question m))]
              (println v m id)
              {:status 200
               :body (merge m (get (availability [id] v) id))}))}}])

(s/def ::user-id spec/string?)
(s/def ::selected-response spec/int?)

(defn quizz-tx [question-id user-id]
  (-> (select :success) (from :quizz)
      (hsql/where [:= :question question-id] [:= :user user-id])
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
              (execute-query!)))])
  (= (-> (correct-response question-id) first :correct_response) selected-answer))

(defn attempt! [question-id user-id success?]
  (let [tx (quizz-tx question-id user-id)
        where-cond (where [:= :question question-id] [:= :user user-id])
        update-query
        (-> (hsql/update :quizz)
            (hsql/sset
             {:success (or (pos? (-> tx first :success)) success?)
              :attempt (sql/call :+ :attempt (if (pos? (-> tx first :success)) 0 1))})
            (where [:= :question question-id] [:= :user user-id]))
        insert-query
        (-> (hsql/insert-into :quizz)
            (hsql/values [{:question question-id :user user-id :attempt 1
                           :success success?}]))
        query (if (seq tx) update-query insert-query)]
    #_(println query (seq tx) (-> tx first :success))
    (execute-query! (sql/format query))))

(def routes-answer
  ["/answer"
   {:post {:summary "Save an answer from the user and return if the selected answer was correct."
           :parameters {:body (s/keys :req-un [::user-id ::selected-answer])}
           :handler
           (fn [m]
             (let [{:keys [user-id selected-answer]} (get-in m [:parameters :body])
                   {:keys [chapter question]} (get-in m [:parameters :path])
                   id (str chapter "_" question)
                   success? (correct-answer? id user-id selected-answer)]
               (println user-id selected-answer id success?)
               (attempt! id user-id success?)
               {:status 200
                :body {:is_correct_response success?}}))}}])

(def routes
  ["/question/:chapter/:question"
   {:coercion reitit.coercion.spec/coercion
    :parameters {:path (s/keys :req-un [::chapter ::question])}}
   routes-availability
   routes-answer])

(comment
  (-> (client/get "http://localhost:3000") :body)
  (->> (client/get "http://localhost:3000/question/0/1/available")
       :body
       (mc/decode mi "application/json"))

  (-> (client/put
       "http://localhost:3000/question/0/1/available"
       {:content-type :json
        :body (json/write-str {:availability true})})
      :body
      (json/read-str :key-fn keyword))

  (-> (client/post
       "http://localhost:3000/question/0/1/answer"
       {:content-type :json
        :body (json/write-str {:user-id "1" :selected-answer 2})})
      :body
      (json/read-str :key-fn keyword))

  (-> (client/get "http://localhost:3001/echo") :body)
  (doseq [chapter (range 4)]
    (convert-question chapter))
  (doseq [chapter (range 4)]
    (import-question->db chapter))

  (def chapter 0)
  (def questions (-> (import-question chapter) (json/read-str :key-fn keyword)))

  (let [{:keys [a b]} {:a 2 :b 2}]
    (println a b))
  #_(map-indexed (fn [i q] (format-question->db q chapter i)) questions)

  )
