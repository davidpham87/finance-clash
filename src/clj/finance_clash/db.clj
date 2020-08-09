(ns finance-clash.db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [datomic.api :as d]))

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
    :db/unique :db/identity
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

   ;; points
   {:db/ident :lecture/homework
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "Questions belonging to the lecture."}

   {:db/ident :lecture.homework/exam?
    :db/valueType :db.type/boolean
    :db/doc "Whether the homework set is an exam."}

   {:db/ident :lecture.homework/start
    :db/valueType :db.type/instant
    :db/doc "The beginning date of the homework."}

   {:db/ident :lecture.homework/deadline
    :db/valueType :db.type/instant
    :db/doc "Last time to return the homework"}

   {:db/ident :lecture.homework/quizz
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "Quizz from which the homework is taken from."}

   {:db/ident :lecture.homwork/shuffle?
    :db/valueType :db.type/boolean
    :db/doc "Whether to shuffle the questions."}

   ;; questions
   {:db/ident :question/tags
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/many
    :db/doc "Tags of the question (category, year)"}

   {:db/ident :question/difficulty
    :db/valueType :db.type/keyword
    :db/doc "Difficulty of the question. One of [:easy, :medium, :hard]"}

   {:db/ident :question/points
    :db/valueType :db.type/keyword
    :db/doc "Number of points (in order to override the default difficulty points)"}

   {:db/ident :question/question
    :db/valueType :db.type/string
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
    :db/doc "Markdown string to explain the answer."}

   ;; quizz
   {:db/ident :quizz/tags
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/many
    :db/doc "Tags of the quizz (category, year)"}

   {:db/ident :quizz/questions
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "Questions belonging to the quizz."}])
