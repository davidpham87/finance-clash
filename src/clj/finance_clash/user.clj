(ns finance-clash.user
  (:require
   [buddy.hashers :as hashers]
   [clj-http.client :as client]
   [clojure.data.json :as json]
   [clojure.pprint :refer (pprint)]
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [finance-clash.auth :as auth :refer (sign-user protected-interceptor unsign-user)]
   [finance-clash.budget :as budget]
   [finance-clash.db :refer (execute-query!)]
   [finance-clash.quiz :refer (latest-series)]
   [honeysql.core :as sql]
   [honeysql.helpers :as hsql :refer (select where from)]
   [muuntaja.core :as mc]
   [muuntaja.format.yaml :as yaml]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]
   [reitit.coercion.spec]
   [spec-tools.spec :as spec]))

(defn ok [d] {:status 200 :body d})
(defn bad-request [d] {:status 400 :body d})

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

(defn get-user
  "Get user by username"
  [id]
  (-> (select :*) (from :user) (where [:= :id id]) (hsql/limit 1) sql/format
      execute-query! first))

(defn login-tx [user]
  (let [full-user (get-user (:id user))]
    (if (and full-user
             (hashers/check (:password user) (:password full-user)))
      (-> (dissoc full-user :password)
          (assoc :token (sign-user user)))
      ::unmatch-credentials)))

(defn login [m]
  (let [user (get-in m [:parameters :body])
        user-tx (login-tx user)]
    (if-not (= user-tx ::unmatch-credentials)
      {:status 200 :body {:user user-tx}}
      {:status 409 :body {:message "Your username or password is not correct."}})))

(defn register-tx [user]
  (let [user-data (-> (select-keys user [:id :password])
                      (update :password hashers/derive {:alg :bcrypt+sha512})
                      (assoc :username (:id user)))
        user-already-exist? (get-user (:id user-data))
        insert-user-query (-> (hsql/insert-into :user)
                              (hsql/values [user-data])
                              sql/format)]
    (if user-already-exist?
      nil
      (do
        (execute-query! insert-user-query)
        user-data))))

(defn register [m]
  (let [user (get-in m [:parameters :body])
        user (register-tx user)]
    (if user
      {:status 200
       :body {:message "Register"
              :user (-> user (dissoc :password) (assoc :token (sign-user user)))}}
      {:status 409 :body {:message "User already exists"}})))

#_(defn update-tx [user])


;; Get/set username from id
;; authentificate? no password
(s/def ::username (s/and string? seq))
(s/def ::series spec/integer?)
(s/def ::password spec/string?)
(s/def ::email spec/string?)
(s/def ::id (s/and string? seq))
(s/def ::credentials (s/keys :req-un [::id ::password]))

(defn answered-questions [user-id series]
  (-> {:select [:question]
       :from [:quiz_attempt]
       :where [:and [:= :series series] [:= :user user-id] [:= :success true]]}
      sql/format
      execute-query!))

(def user
  [["/user" {:coercion reitit.coercion.spec/coercion
             :get {:summary "User Hello message"
                   :handler (fn [m] {:status 200 :body "Hello"})}
             :put {:summary "User login"
                   :parameters {:body ::credentials}
                   :handler login}
             :post {:summary "User login"
                    :parameters {:body ::credentials}
                    :handler register}}]
   ["/user" {:coercion reitit.coercion.spec/coercion}
    ["/:id"
     ["" {:get {:summary "Get username"
                :handler
                (fn [{{id :id} :path-params}]
                  {:status 200
                   :body (username id)})}
          :put {:summary "Set username"
                :interceptors [protected-interceptor]
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
     ["/wealth"
      {:get {:summary "Retrieve wealth of user"
             :interceptors [protected-interceptor]
             :handler
             (fn [{{user-id :id} :path-params}]
               {:status 200
                :body (budget/budget user-id)})}}]
     ["/answered-questions"
      {:get {:parameters {:query (s/keys :opt-un [::series])}
             :handler
             (fn [m]
               (let [user-id (get-in m [:path-params :id])
                     series (or (get-in m [:parameters :query :series])
                                (-> (first (execute-query! (latest-series))) :id))]
                 {:status 200
                  :body (answered-questions user-id series)}))}}]]]])

(def routes user)

(comment
  (-> (client/get "http://localhost:3000") :body)

  (-> (client/get "http://localhost:3000/echo") :body)
  (-> (client/get "http://localhost:3000/spec") :body)
  (-> (client/get "http://localhost:3000/user/1/answered-questions?series=1")
      :body
      (json/read-str :key-fn keyword))

  (-> (client/get "http://localhost:3000/user/neo2551/wealth")
      :body
      (json/read-str :key-fn keyword))

  (-> (client/put
       "http://localhost:3000/user/2"
       {:content-type :json
        :headers {:Authorization (str "Token " "eyJhbGciOiJIUzUxMiJ9.eyJ1c2VyIjoibmVvMjU1MSJ9.QQwCTk9aO75s62i2skKyVqSIKjZ0YHH6Ivyaysk7hkKUyQfYu0Ag29kDe-2FdQuwAxLRqtDrO_5I9GYfJRofAQ")}
        :body (json/write-str {:username "Vincent"})})
      :body
      (json/read-str :key-fn keyword))

  (-> (client/put
       "http://localhost:3000/user"
       {:content-type :json
        :headers {:Authorization (str "Token " "eyJhbGciOiJIUzUxMiJ9.eyJ1c2VyIjoibmVvMjU1MSJ9.QQwCTk9aO75s62i2skKyVqSIKjZ0YHH6Ivyaysk7hkKUyQfYu0Ag29kDe-2FdQuwAxLRqtDrO_5I9GYfJRofAQ")}
        :body (json/write-str {:id "neo2551" :password "hello_world"})})
      :body
      (json/read-str :key-fn keyword))
  (unsign-user "eyJhbGciOiJIUzUxMiJ9.eyJ1c2VyIjoibmVvMjU1MSJ9.QQwCTk9aO75s62i2skKyVqSIKjZ0YHH6Ivyaysk7hkKUyQfYu0Ag29kDe-2FdQuwAxLRqtDrO_5I9GYfJRofAQ")
  (get-user "neo2551")
  (-> (client/post
       "http://localhost:3000/user"
       {:content-type :json
        :body (json/write-str {:id "neo2551" :password "hello_world"})})
      :body
      (json/read-str :key-fn keyword))

  (budget/earn "1" 100)
  (username "1")
  (username "2")
  (username "2" "Vincent")
  (username "3")
  (username "3" "")
  (get-user "Neo")
  (doseq [chapter (range 4)]
    (convert-question chapter)))
