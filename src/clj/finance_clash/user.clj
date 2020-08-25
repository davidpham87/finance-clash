(ns finance-clash.user
  (:require
   [buddy.hashers :as hashers]
   [clj-http.client :as client]
   [clojure.data.json :as json]
   [clojure.spec.alpha :as s]
   [datomic.api]
   [finance-clash.auth :as auth :refer (sign-user protected-interceptor unsign-user)]
   [finance-clash.budget :as budget]
   [finance-clash.db :refer (execute-query!)]
   [finance-clash.quiz :refer (latest-series)]
   [honeysql.core :as sql]
   [honeysql.helpers :as hsql :refer (select where from)]
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

(defn update-user!
  [{:keys [id] :as user-data}]
  (let [user-data (if (:password user-data)
                    (update user-data :password hashers/derive {:alg :bcrypt+sha512})
                    (dissoc user-data :password))
        user-data (if (:username user-data) user-data (dissoc user-data :username))]
    (when (or (:password user-data) (:username user-data))
      (-> (hsql/update :user) (hsql/sset user-data) (where [:= :id id])
          sql/format execute-query! first))))

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
        (budget/budget-init (:id user-data) 100)
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
             :put {:summary "Login user"
                   :parameters {:body ::credentials}
                   :handler login}
             :post {:summary "Register user"
                    :parameters {:body ::credentials}
                    :handler register}}]
   ["/user" {:coercion reitit.coercion.spec/coercion}
    ["/:id"
     ["" {:get {:summary "Get user"
                :handler
                (fn [{{id :id} :path-params}]
                  {:status 200
                   :body (dissoc (get-user id) :password)})}
          :put {:summary "Set user"
                :interceptors [protected-interceptor]
                :parameters {:body (s/keys :opt-un [::username ::password])}
                :handler
                (fn [m]
                  (let [id (get-in m [:path-params :id])
                        {:keys [username password]} (get-in m [:parameters :body])]
                    (update-user! {:id id :username username :password password})
                    {:status 200 :body {:id id :username username}}))}}]
     ["/wealth"
      {:get {:summary "Retrieve wealth of user"
             ;; :interceptors [protected-interceptor]
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
  (def token "eyJhbGciOiJIUzUxMiJ9.eyJ1c2VyIjoibmVvMjU1MSJ9.QQwCTk9aO75s62i2skKyVqSIKjZ0YHH6Ivyaysk7hkKUyQfYu0Ag29kDe-2FdQuwAxLRqtDrO_5I9GYfJRofAQ")

  (-> (client/get "http://localhost:3000") :body)

  (-> (client/get "http://localhost:3000/echo") :body)
  (-> (client/get "http://localhost:3000/spec") :body)
  (-> (client/get "http://localhost:3000/user/1/answered-questions?series=1")
      :body
      (json/read-str :key-fn keyword))

  (-> (client/get "http://localhost:3000/user/neo2551/wealth"
                  {:content-tpye :json
                   :headers {:Authorization (str "Token " token)}})
      :body
      (json/read-str :key-fn keyword))

  (budget/budget-init "neo2551" 500)
  (budget/earn! "neo2551" 500)
  (budget/wealth "neo2551")

  (-> (client/put
       "http://localhost:3000/user/2"
       {:content-type :json
        :headers {:Authorization (str "Token " token)}
        :body (json/write-str {:username "Beck"})})
      :body
      (json/read-str :key-fn keyword))

  (-> (client/post
       "http://localhost:3000/user"
       {:content-type :json
        :body (json/write-str {:id "admin" :password "welcome_finance"})})
      :body
      (json/read-str :key-fn keyword))

  (-> (client/post
       "http://localhost:3000/user"
       {:content-type :json
        :body (json/write-str {:id "vincent_beck" :password "welcome_finance"})})
      :body
      (json/read-str :key-fn keyword))

  (-> (client/post
       "http://localhost:3000/user"
       {:content-type :json
        :body (json/write-str {:id "neo2551" :password "welcome_finance"})})
      :body
      (json/read-str :key-fn keyword))

  (unsign-user token)
  (get-user "neo2551")

  (-> (client/post
       "http://localhost:3000/user"
       {:content-type :json
        :body (json/write-str {:id "neo2558" :password "hello_world"})})
      :body
      (json/read-str :key-fn keyword))

  (def reset-wealth! []
    (let  [users (-> {:select [:id] :from [:user]}
                     sql/format
                     (execute-query!)
                     (as-> m (mapv :id m)))])
    (doseq [u users]
      (budget/budget-init u 100)))

  (budget/ranking)

  (budget/earn "1" 100)
  (username "1")
  (username "2")
  (username "2" "Vincent")
  (username "3")
  (username "3" "")
  (get-user "admin")

  #_(register-tx {:id "admin" :password "welcome_finance"})
  (doseq [chapter (range 4)]
    (convert-question chapter)))
