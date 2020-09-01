(ns finance-clash.user
  (:require
   [buddy.hashers :as hashers]
   [clj-http.client :as client]
   [clojure.data.json :as json]
   [clojure.spec.alpha :as s]
   [datomic.api :as d]
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
  (-> (d/q '[:find (pull ?e [:user/id :user/password])
             :in $ ?id
             :where [?e :user/id ?id]]
           (finance-clash.db/get-db)
           id)
      ffirst))

(defn update-user!
  [{:keys [id] :as user-data}]
  (let [user-data (if (:password user-data)
                    (update user-data :password hashers/derive {:alg :bcrypt+sha512})
                    (dissoc user-data :password))
        user-data (if (:username user-data) user-data (dissoc user-data :username))]
    (when (or (:password user-data) (:username user-data))
      (-> (hsql/update :user) (hsql/sset user-data) (where [:= :id id])
          sql/format execute-query! first))))

(defn login-tx [{:keys [id password] :as user}]
  (let [full-user (first (get-user (:id user)))
        full-user (when full-user
                    (d/pull (finance-clash.db/get-db)
                            [:user/id :user/password]
                            full-user))]
    (if (and full-user
             (hashers/check (:password user) (:user/password full-user)))
      (-> (dissoc full-user :user/password)
          (assoc :user/token (sign-user user)))
      ::unmatch-credentials)))

(defn login [m]
  (let [user (get-in m [:parameters :body])
        user-tx (login-tx user)]
    (if-not (= user-tx ::unmatch-credentials)
      {:status 200 :body {:user user-tx}}
      {:status 409 :body {:message "Your username or password is not correct."}})))

(defn register-tx [{:keys [id password] :as user}]
  (let [user-data (-> (select-keys user [:id :password])
                      (update :password hashers/derive {:alg :bcrypt+sha512})
                      (assoc :name (:id user)))
        user-data (reduce-kv (fn [m k v] (assoc m (keyword "user" (name k)) v)) {} user-data)
        already-exist? (get-user (:user/id user-data))]
    (if already-exist?
      :user/already-exists
      {:tx-data (d/transact
                 @finance-clash.db/conn
                 [(-> user-data
                      (assoc :user/transactions {:user.transactions/amount 100
                                                 :user.transactions/reason "Initial"}))])
       :user-data user-data})))

(comment
  (d/transact @finance-clash.db/conn
              [{:user/id "David"
                :user/name "David"
                :user/password (hashers/derive "hello" {:alg :bcrypt+sha512})}])

  (d/q {:find '[?e]
        :where '[[?e :user/id "Henry"]]}
   (finance-clash.db/get-db))
  (get-user "Vincent")
  (register-tx {:id "Neo" :password "Hello"})
  (register-tx {:id "Vincent" :password "Hello"})

  (register {:parameters {:body {:id "Vincent2" :password "Hello"}}})

  (login-tx {:id "Neo" :password "Hello"})
  (login-tx {:id "Vincent" :password "Hello"})
  (login {:parameters {:body {:id "Neo" :password "Hello"}}})
  )

(defn register [m]
  (let [user (get-in m [:parameters :body])
        user-data (register-tx user)]
    (if (not= user-data :user/already-exists)
      {:status 200
       :body {:message "Register"
              :user (-> user-data :user-data (dissoc :password) (assoc :user/token (sign-user user)))}}
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
