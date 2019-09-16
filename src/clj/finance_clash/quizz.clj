(ns finance-clash.quizz
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

;; Import data
;; This should be on its own namespace ideally.
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
             success? (correct-answer? id user-id selected-answer)]
         #_(println user-id selected-answer id success? series)
         (attempt! id user-id series success?)
         {:status 200
          :body {:is_correct_response success?}}))}}])

;; Series
;; Series are a set of question which can only be release after the release date.
(s/def ::availability spec/boolean?)
(s/def ::release-date spec/string?)

(defn availability
  "Query availability of question given their ids (chapter_number)"
  ([ids]
   (let [release-dates
         (-> (select :release_date) (from :quizz_series) (where [:in :series ids])
             (sql/format) execute-query!)]
     (zipmap ids release-dates)))
  ([ids date weight new?]
   (let [insert-query
         (-> (hsql/insert-into :quizz_series)
             (hsql/values (mapv #(assoc {:release-date date :weight weight}
                                        :series %) ids)))
         update-query
         (-> (hsql/update :quizz_series) (hsql/sset {:release_date date})
             (where [:in :series ids]))
         release-dates (execute-query! (sql/format (if new? insert-query update-query)))]
     (zipmap ids release-dates))))

(defn availability-update-handler [m]
  (let [method (:request-method m)
        {:keys [release-date weight]} (get-in m [:parameters :body])
        id (get-in m [:parameters :path :series])]
    (println release-date id)
    (availability [id] release-date (or weight 1) (= :post method))
    (println method)
    {:status 200
     :body {:series id}}))

(def routes-availability
  ["/available"
   {:get {:summary "Retrieve series availability for a given chapter and number."
          :handler
          (fn [{{{:keys [series]} :path} :parameters}]
            (let [release-date (get (availability [series]) series)]
              {:status 200
               :body {:series series :availability
                      (not (pos?
                           (compare (:release_date release-date)
                                    (jt/format "yyyy-MM-dd" (jt/local-date)))))}}))}
    :put {:summary "Update series availability for a given chapter and number."
          :parameters {:body (s/keys :req-un [::release-date]
                                     :opt-un [::weight])}
          :handler availability-update-handler}
    :post {:summary "Create series availability for a given chapter and number."
           :parameters {:body (s/keys :req-un [::release-date]
                                      :opt-un [::weight])}
           :handler availability-update-handler}}])


(def routes-series
  ["/series/:series"
   {:coercion reitit.coercion.spec/coercion
    :parameters {:path (s/keys :req-un [::series])}
    :get {:summary "Get series details"
          :handler (fn [m] {:status 200 :body {:series 0}})}}
   routes-availability])

(def routes
  [["/quizz/:chapter/:question"
    {:coercion reitit.coercion.spec/coercion
     :parameters {:path (s/keys :req-un [::chapter ::question])}}
    routes-answer]
   routes-series])

(comment
  (-> (client/get "http://localhost:3000") :body)
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
