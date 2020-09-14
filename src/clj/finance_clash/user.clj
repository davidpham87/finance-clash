(ns finance-clash.user
  (:require
   [buddy.hashers :as hashers]
   [clj-http.client :as client]
   [clojure.data.json :as json]
   [clojure.spec.alpha :as s]
   [datomic.api :as d]
   #_[datahike.api :as d]
   [finance-clash.auth :as auth :refer (sign-user protected-interceptor unsign-user)]
   [finance-clash.budget :as budget]
   [finance-clash.db]
   [finance-clash.quiz :refer (latest-series)]
   [reitit.coercion.spec]
   [spec-tools.spec :as spec]))

(defn ok [d] {:status 200 :body d})
(defn bad-request [d] {:status 400 :body d})

(defn username
  "Get and set username, do not create new user, if no username"
  ([id]
   (d/q '[:find ?n
          :keys username
          :args $ ?id
          :where
          [?e :user/id ?id]
          [?e :user/name ?n]]
        (finance-clash.db/get-db)
        id))
  ([id s]
   (d/transact (finance-clash.db/conn) [{:user/id id :user/name s}])))

(defn get-user
  "Get user with id"
  [id]
  (-> (d/q '[:find (pull ?e [:user/id :db/id :user/password :user/name])
             :in $ ?u
             :where [?e :user/id ?u]]
           (finance-clash.db/get-db)
           id)
      ffirst))

(defn update-user!
  [{:keys [id password username wealth]}]
  (let [password (when password (hashers/derive password {:alg :bcrypt+sha512}))
        user-data #:user{:id id :password password :name username}]
    (when (or password username)
      (d/transact (finance-clash.db/get-conn) [user-data]))
    (when wealth
      (budget/budget id wealth))))

(defn login-tx [{:keys [id password] :as user}]
  (let [full-user (get-user (:id user))]
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
                                                 :user.transactions/reason "Initial"}
                             :user/score 100))])
       :user-data user-data})))

(defn init-users []
  [(register-tx {:id "admin" :password "hello_finance"})
   (register-tx {:id "neo" :password "hello"})
   (register-tx {:id "neo2551" :password "hello"})
   (register-tx {:id "vincent" :password "hello_finance"})])

(comment
  (d/transact @finance-clash.db/conn
              [{:user/id "David"
                :user/name "David"
                :user/password (hashers/derive "hello" {:alg :bcrypt+sha512})}])
  (init-users)

  (d/q {:find '[?e]
        :where '[[?e :user/id "Henry"]]}
   (finance-clash.db/get-db))
  (get-user "Vincent")
  (d/pull (finance-clash.db/get-db) '[*] (:db/id (get-user "neo")))
  (update-user! {:id "David" :password "hello" :username "Luke"})
  (get-user "Neo")
  (get-user "David")
  (login-tx {:id "David" :password "what?"})
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
(s/def ::wealth spec/int?)

(s/def ::id (s/and string? seq))
(s/def ::credentials (s/keys :req-un [::id ::password]))

(defn answered-questions [user-id series-id]
  (->> (d/q '[:find ?q ?correct
              :in $ ?u ?p
              :where
              [?p :problems/transactions ?t]
              [?t :transaction/question ?q]
              [?t :transaction/user ?u]
              [?t :transaction/correct? ?correct]]
            (finance-clash.db/get-db)
            [:user/id user-id]
            series-id)
       (group-by first) ;; group-by question-id
       (reduce-kv
        (fn [m k v]
          (assoc m k (reduce (fn [acc x] (or acc (second x))) false v)))
        {})
       (into [] (comp (filter #(= (second %) true)) (map first)))
       (d/pull-many (finance-clash.db/get-db) [:db/id :question/title])))

(comment
  (answered-questions "neo" "First"))

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
          :put
          {:summary "Set user"
           :interceptors [protected-interceptor]
           :parameters {:body (s/keys :opt-un [::username ::password ::wealth])}
           :handler
           (fn [m]
             (let [token-id (:identity m)
                   id (get-in m [:path-params :id])
                   {:keys [username password wealth]} (get-in m [:parameters :body])]
               (println token-id)
               (case token-id
                 ({:user "admin"} {:user "neo2551"})
                 (do
                   (update-user! {:id id :wealth wealth :password password})
                   {:status 200 :body #:user{:id id :wealth wealth}})

                 {:user id}
                 (do
                   (update-user! {:id id :username username :password password})
                   {:status 200 :body #:user{:id id :name username
                                             :token token-id}})
                 {:status 403 :body {:error "Unauthorized"}})))}

          :delete
          {:summary "Retract users"
           :interceptors [protected-interceptor]
           :handler
           (fn [m]
             (let [token-id (:identity m)
                   id (get-in m [:path-params :id])
                   {:keys [username password wealth]} (get-in m [:parameters :body])]
               (if (= token-id {:user id})
                 (do
                   (update-user!
                    {:id id :username username :password password
                     :wealth wealth})
                   {:status 200 :body #:user{:id id :name username
                                             :token token-id}})
                 {:status 403 :body {:error "Unauthorized"}})))}}]
     ["/wealth"
      {:get {:summary "Retrieve wealth of user"
             ;; :interceptors [protected-interceptor]
             :handler
             (fn [{{user-id :id} :path-params :as m}]
               (if (= (:identity m) {:user user-id})
                 {:status 200
                  :body {:wealth (budget/budget user-id)}}
                 {:status 403
                  :body {:error "Unauthorized"}}))}}]
     ["/answered-questions"
      {:get {:parameters {:query (s/keys :opt-un [::series])}
             :handler
             (fn [m]
               (let [identity (get-in m [:identity])
                     user-id (get-in m [:path-params :id])
                     series (or (get-in m [:parameters :query :series])
                                (:db/id (latest-series)))]
                 (if (= identity {:user user-id})
                   {:status 200
                    :body (answered-questions user-id series)}
                   {:status 403
                    :body {:error "Unauthorized"}})))}}]]]])

(def routes user)

(comment

  (def token "eyJhbGciOiJIUzUxMiJ9.eyJ1c2VyIjoibmVvMjU1MSJ9.QQwCTk9aO75s62i2skKyVqSIKjZ0YHH6Ivyaysk7hkKUyQfYu0Ag29kDe-2FdQuwAxLRqtDrO_5I9GYfJRofAQ")
  (finance-clash.auth/unsign-user token)
  (-> (client/get "http://localhost:3000") :body)

  (-> (client/get "http://localhost:3000/echo") :body)
  (-> (client/get "http://localhost:3000/spec") :body)
  (-> (client/get "http://localhost:3000/user/1/answered-questions?series=1")
      :body
      #_(json/read-str :key-fn keyword))

  (-> (client/get "http://localhost:3000/user/neo2551/wealth"
                  {:content-tpye :json
                   :headers {:Authorization (str "Token " token)}})
      :body
      (json/read-str :key-fn keyword))

  (budget/budget-init "neo2551" 500)
  (budget/budget "neo2551")
  (budget/earn! "neo2551" 500)
  (budget/wealth "neo2551")

  (-> (client/put
       "http://localhost:3000/user/vincent_beck"
       {:content-type :json
        :headers {:Authorization (str "Token " token)}
        :body (json/write-str {:username "Pham"})})
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
  (get-user "vincent_beck")

  (-> (client/post
       "http://localhost:3000/user"
       {:content-type :json
        :body (json/write-str {:id "neo2558" :password "hello_world"})})
      :body
      (json/read-str :key-fn keyword))

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
