(ns finance-clash.user
  (:require [clj-http.client :as client]
            [clojure.pprint :refer (pprint)]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [finance-clash.db :refer (execute-query!)]
            [finance-clash.budget :as budget]
            [honeysql.core :as sql]
            [honeysql.helpers :as hsql :refer (select where from)]
            [muuntaja.core :as mc]
            [muuntaja.format.yaml :as yaml]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [reitit.coercion.spec]
            [spec-tools.spec :as spec]))

(defn username
  "Get and set username, do not create new user, if no username"
  ([id]
   (-> (select :username) (from :user) (where [:= :id id])
       sql/format
       execute-query!
       first))
  ([id s]
   (-> (hsql/update :user)
       (hsql/sset {:username s})
       (where [:= :id id])
       sql/format
       execute-query!
       first)))

;; Get/set username from id
;; authentificate? no password
(s/def ::username (s/and string? seq))
(s/def ::series spec/integer?)

(defn answered-questions [user-id series]
  (-> {:select [:question]
       :from [:quizz_attempt]
       :where [[:= :series series] [:= :user user-id] [:= :success true]]}
      sql/format
      execute-query!))

(def user
  [["/user" {:coercion reitit.coercion.spec/coercion
             :get {:summary "User Hello message"
                   :handler (fn [m] {:status 200 :body "Hello"})}}]
   ["/user" {:coercion reitit.coercion.spec/coercion}
    ["/:id"
     {:get {:summary "Get username"
            :handler
            (fn [{{id :id} :path-params}]
              {:status 200
               :body (username id)})}
      :put {:summary "Set username"
            :parameters {:body (s/keys :req-un [::username])}
            :handler
            (fn [m]
              (let [id (get-in m [:path-params :id])
                    username-input (get-in m [:parameters :body :username])]
                (if username-input
                  (do
                    (username id username-input)
                    {:status 200
                     :body {:username username-input}})
                  {:status 422
                   :body {:message "Missing username"}})))}}]
    ["/:id/wealth" {:get {:handler (fn [{{user-id :id} :path-params}]
                                       (budget/budget user-id))}}]
    ["/:id/answered-questions"
     {:get {:parameters {:query (s/keys :req-un [::series])}
            :handler
            (fn [m]
              (let [user-id (get-in m [:path-params :id])
                    series (get-in m [:parameters :query :series])]
                {:status 200
                 :body (answered-questions user-id series)}))}}]]])

(def routes user)

(comment
  (-> (client/get "http://localhost:3000") :body)

  (-> (client/get "http://localhost:3000/echo") :body)
  (-> (client/get "http://localhost:3000/spec") :body)
  (-> (client/get "http://localhost:3000/user/1")
      :body
      (json/read-str :key-fn keyword))

  (-> (client/put
       "http://localhost:3000/user/1"
       {:content-type :json
        :body (json/write-str {:username "David"})})
      :body
      (json/read-str :key-fn keyword))

  (username "1")
  (username "2")
  (username "2" "Vincent")
  (username "3")
  (username "3" "")

  (doseq [chapter (range 4)]
    (convert-question chapter)))
