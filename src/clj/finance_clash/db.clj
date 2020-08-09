(ns finance-clash.db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            #_[datomic.api :as d]
            [datahike.api :as d]
            [datahike-postgres.core]
            #_[crux.api :as crux]
            #_[crux.jdbc]))

(def db
  {:dbtype "sqlite"
   :dbname "sql/finance-clash-database.db"})

(def ds (jdbc/get-datasource db))

(defn execute-query! [query]
  (with-open [connection (jdbc/get-connection ds)]
    (jdbc/execute! connection query
                   {:builder-fn rs/as-unqualified-maps})))

#_(defn init! []
    (jdbc/execute! ds ["
create table user (
  name varchar(32),
  email varchar(255)
)"])

    (jdbc/execute! ds ["
create table quiz_attempts (
  user integer,
  question integer,
  chapter integer,
  start varchar(32),
  end varchar(32),
)"])

    #_(jdbc/execute! ds ["
drop table user
"])

    (jdbc/execute! ds ["
insert into user(name, email)
  values('David','david@davidolivier.pro')"])

    (jdbc/execute! ds ["
insert into user(name, email)
  values('vincent','vincent@davidolivier.pro')"])

    (jdbc/execute! ds ["
SELECT rowid, * FROM user;
"]))

(def datomic-uri "datomic:free://localhost:4334/finance-clash")

(d/create-database datomic-uri)

(def conn (d/connect datomic-uri))
(def db (d/db conn))

(set! *print-length* 250)

@(d/transact conn [{:db/doc "Hello world"}])

(def finanche-clash-schema
  [{:db/ident :user/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "User name"}

   {:db/ident :user/id
    :db/valueType :db.type/string
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one
    :db/doc "User id"}

   {:db/ident :user/kind
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "Kind of user (professor, student)"}

   {:db/ident :user/lectures
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "Lectures the user to which the user is a member."}

   {:db/ident :lecture/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/many
    :db/doc "Name of the lecture"}

   {:db/ident :lecture/start
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "Start date of the lecture"}

   {:db/ident :lecture/end
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "End date of the lecture"}

   {:db/ident :lecture/templates
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "Templates from which the lecture is taken"}

   {:db/ident :lecture/homework
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "Questions belonging to the lecture."}

   {:db/ident :lecture/exercises
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "Exercises to train students."}

   ;; exercises

   {:db/ident :exercises/title
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "Name of the exercises."}

   {:db/ident :exercises/kind
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "One of :exam, :training, :homework"}

   {:db/ident :exercises/tags
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/many
    :db/doc "Tags of the exercises."}

   {:db/ident :exercises/start
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "The beginning date of the homework."}

   {:db/ident :exercises/deadline
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "Last time to return the homework"}

   {:db/ident :exercises/quizz
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "Quizz from which the exercises is taken from."}

   {:db/ident :exercises/shuffle?
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one
    :db/doc "Whether to shuffle the questions in the exercises."}

   ;; questions
   {:db/ident :question/tags
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/many
    :db/doc "Tags of the question (category, year)"}

   {:db/ident :question/difficulty
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "Difficulty of the question. One of [:easy, :medium, :hard]"}

   {:db/ident :question/points
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "Number of points (in order to override the default difficulty points)"}

   {:db/ident :question/title
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Title of the question."}

   {:db/ident :question/question
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Question that is asked to the student."}

   {:db/ident :question/answers
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/many
    :db/doc "Possible answers to the student."}

   {:db/ident :question/correct-answers
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "Possible answers to the student."}

   {:db/ident :question/explanation
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Markdown string to explain the answer."}

   ;; quizz

   {:db/ident :quizz/tags
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/many
    :db/doc "Tags of the quizz (category, year)"}

   {:db/ident :quizz/title
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Title of the quizz."}

   {:db/ident :quizz/questions
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "Questions belonging to the quizz."}])

(d/transact conn finanche-clash-schema)

(->> (d/q '[:find ?e ?attr
           :in $
           :where
           [?e :db/ident ?attr]]
         (d/db conn))
     (into [])
     (sort-by second))


(comment

  (def crux-db
    {:crux.node/topology '[crux.jdbc/topology]
     :crux.jdbc/dbtype "postgresql"
     :crux.jdbc/dbname   "cruxdb2"
     :crux.jdbc/host     "localhost"
     :crux.jdbc/port     "5432"
     :crux.jdbc/user     "postgres"
     :crux.jdbc/password "postgres"})

  (defn start-jdbc-node []
    (crux/start-node crux-db))

  (crux/submit-tx
   (crux/start-node crux-db)
   [[:crux.tx/put
     {:crux.db/id :dbpedia.resource/Pablo-Picasso ; id
      :name "Pablo"
      :last-name "Picasso"}
     #inst "2018-05-18T09:20:27.966-00:00"]])

  (def node (crux/start-node crux-db))

  (let [db (-> node crux/db)]
    (crux/q db
            '{:find [e]
              :where [[e :name "Pablo"]]}))

  (crux/entity (crux/db node) :dbpedia.resource/Pablo-Picasso)
)


(comment

  (def cfg {:store              {:backend  :pg
                                 :username "postgres"
                                 :password "postgres"
                                 :path     "/datahike"
                                 :host     "localhost"
                                 :port     5432}
            :schema-flexibility :write
            :keep-history?      true})

  (d/create-database cfg)


  (def conn (d/connect cfg))

  (d/transact conn finanche-clash-schema)


  ;; The first transaction will be the schema we are using:
  (d/transact conn [{:db/ident :name
                     :db/valueType :db.type/string
                     :db/cardinality :db.cardinality/one }
                    {:db/ident :age
                     :db/valueType :db.type/long
                     :db/cardinality :db.cardinality/one }])

  ;; Let's add some data and wait for the transaction
  (d/transact conn [{:name  "Alice", :age   20 }
                    {:name  "Bob", :age   30 }
                    {:name  "Charlie", :age   40 }
                    {:age 15 }])

  (d/q '[:find ?e ?n ?a
         :keys entity name age
         :where
         [?e :name ?n]
         [?e :age ?a]]
       @conn)

  #_(d/transact conn [{:db/ident :name
                     :db/valueType :db.type/string
                     :db/cardinality :db.cardinality/one }
                    {:db/ident :age
                     :db/valueType :db.type/long
                     :db/cardinality :db.cardinality/one }])



  )
