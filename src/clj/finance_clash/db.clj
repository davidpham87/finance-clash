(ns finance-clash.db
  (:require [datomic.api :as d]))

(def tags-schema
  "Defines tags."
  [#:db{:ident       :tags/description
        :valueType   :db.type/string
        :cardinality :db.cardinality/one
        :doc         "Description of the value of the tag, e.g. category, country, year"}
   #:db{:ident       :tags/value
        :valueType   :db.type/string
        :unique      :db.unique/identity
        :cardinality :db.cardinality/one
        :doc         "Tags value"}])

(def user-schema
  "Defines a user"
  [#:db{:ident       :user/name
        :valueType   :db.type/string
        :cardinality :db.cardinality/one
        :doc         "User name"}
   #:db{:ident       :user/id
        :valueType   :db.type/string
        :unique      :db.unique/identity
        :cardinality :db.cardinality/one
        :doc         "User id"}
   #:db{:ident       :user/password
        :valueType   :db.type/string
        :unique      :db.unique/identity
        :cardinality :db.cardinality/one
        :doc         "Hashed password"}

   #:db{:ident       :user/kind
        :valueType   :db.type/ref
        :cardinality :db.cardinality/one
        :isComponent true
        :doc         "Kind of user [:user.kind/professor, :user.kind/student]"}
   #:db{:ident :user.kind/professor}
   #:db{:ident :user.kind/student}

   #:db{:ident       :user/transactions
        :valueType   :db.type/ref
        :cardinality :db.cardinality/many
        :isComponent true
        :doc         "Transactions for scores"}

   ;; to be redefined.
   #:db{:ident       :user.transactions/amount
        :valueType   :db.type/long
        :cardinality :db.cardinality/one
        :doc         "Amount of transactions"}

   #:db{:ident       :user.transactions/account
        :valueType   :db.type/ref
        :cardinality :db.cardinality/one
        :isComponent true
        :doc         "Which account the transactions is made on. For now, it will be the
       lectures."}

   #:db{:ident       :user.transactions/reason
        :valueType   :db.type/string
        :cardinality :db.cardinality/one
        :doc         "Comment on the transaction."}])

(def lecture-schema
  "A lecture is a collection of homeworks and students with a given defined
  time window. Transactions are the answers by the students. It should be
  mainly used for homework."
  [#:db{:ident       :lecture/id
        :valueType   :db.type/string
        :unique      :db.unique/value
        :cardinality :db.cardinality/one
        :doc
        "Identifier of the lecture. Usually each owner should have a unique ID."}

   #:db{:ident       :lecture/title
        :valueType   :db.type/string
        :cardinality :db.cardinality/one
        :doc         "Name of the lecture"}

   #:db{:ident       :lecture/professors
        :valueType   :db.type/ref
        :cardinality :db.cardinality/many
        :isComponent true
        :doc         "Professor(s) of the lecture"}

   #:db{:ident       :lecture/students
        :valueType   :db.type/ref
        :cardinality :db.cardinality/many
        :isComponent true
        :doc         "Students following the lecture."}

   #:db{:ident       :lecture/start
        :valueType   :db.type/instant
        :cardinality :db.cardinality/one
        :doc         "Start date of the lecture"}

   #:db{:ident       :lecture/end
        :valueType   :db.type/instant
        :cardinality :db.cardinality/one
        :doc         "End date of the lecture"}

   #:db{:ident       :lecture/templates
        :valueType   :db.type/ref
        :cardinality :db.cardinality/many
        :doc         "Templates from which the lecture is taken"}

   #:db{:ident       :lecture/problems
        :valueType   :db.type/ref
        :isComponent true
        :cardinality :db.cardinality/many
        :doc         "Problem set offered to students for training or examination."}

   #:db{:ident       :lecture/tags
        :valueType   :db.type/ref
        :cardinality :db.cardinality/many
        :doc         "Additional tags for the lecture for identifying them."}

   #:db{:ident       :problems/title
        :valueType   :db.type/instant
        :cardinality :db.cardinality/one
        :doc         "Title of the set of problems."}

   #:db{:ident       :problems/kind
        :valueType   :db.type/keyword
        :cardinality :db.cardinality/one
        :doc         "One of :exam :training :homework"}

   #:db{:ident       :problems/tags
        :valueType   :db.type/ref
        :cardinality :db.cardinality/many
        :doc         "Tags of the problems."}

   #:db{:ident       :problems/start
        :valueType   :db.type/instant
        :cardinality :db.cardinality/one
        :doc         "The beginning date of the problem set."}

   #:db{:ident       :problems/deadline
        :valueType   :db.type/instant
        :cardinality :db.cardinality/one
        :doc         "Last time to return the problem set"}

   #:db{:ident       :problems/questions
        :valueType   :db.type/ref
        :isComponent true
        :cardinality :db.cardinality/many
        :doc         "Questions of the problem set. You might want to add questions or
        modify questions that are in the quizzes template."}

   #:db{:ident       :problems/shuffle?
        :valueType   :db.type/boolean
        :cardinality :db.cardinality/one
        :doc         "Whether to shuffle the questions in the problems."}

   #:db{:ident       :problems/transactions
        :valueType   :db.type/ref
        :cardinality :db.cardinality/many
        :doc         "All the answers provided by the student."}

   #:db{:ident       :transaction/user
        :valueType   :db.type/ref
        :cardinality :db.cardinality/one
        :doc         "User who answered"}

   #:db{:ident       :transaction/question
        :valueType   :db.type/ref
        :cardinality :db.cardinality/one
        :doc         "Question which was answered."}

   #:db{:ident       :transaction/answer
        :valueType   :db.type/string
        :cardinality :db.cardinality/one
        :doc         "Answer from the user."}

   #:db{:ident       :transaction/correct?
        :valueType   :db.type/boolean
        :cardinality :db.cardinality/one
        :doc         "Whether the answer has been accepted as correct or not."}])

(def quiz-schema
  "Quizzes are collection of questions allowing to group questions for reuse."
  [#:db{:ident       :quiz/title
        :valueType   :db.type/string
        :cardinality :db.cardinality/one
        :doc         "Title of the quiz."}
   #:db{:ident       :quiz/tags
        :valueType   :db.type/ref
        :cardinality :db.cardinality/many
        :doc         "Tags of the quiz (category year)."}
   #:db{:ident       :quiz/questions
        :valueType   :db.type/ref
        :isComponent true
        :cardinality :db.cardinality/many
        :doc         "Questions belonging to the quiz."}])

(def question-answer-schema
  "Questions and answers schema. A question can have be either a multiple choice
  or a single string question. The answer can be either multiple or unique."
  [#:db{:ident       :question/tags
        :valueType   :db.type/ref
        :isComponent true
        :cardinality :db.cardinality/many
        :doc         "Tags of the question (category year)."}

   #:db{:ident       :question/difficulty
        :valueType   :db.type/ref
        :isComponent true
        :cardinality :db.cardinality/one
        :doc         "Difficulty of the question."}
   #:db{:ident :question.difficulty/easy}
   #:db{:ident :question.difficulty/medium}
   #:db{:ident :question.difficulty/hard}

   #:db{:ident       :question/points
        :valueType   :db.type/long
        :cardinality :db.cardinality/one
        :doc         "Number of points (in order to override the default difficulty points)"}

   #:db{:ident       :question/duration
        :valueType   :db.type/long
        :cardinality :db.cardinality/one
        :doc         "Duration of the question in seconds. Max 2^63-1."}

   #:db{:ident       :question/title
        :valueType   :db.type/string
        :cardinality :db.cardinality/one
        :doc         "Title of the question."}

   #:db{:ident       :question/question
        :valueType   :db.type/string
        :cardinality :db.cardinality/one
        :doc         "Question that is asked to the student."}

   #:db{:ident       :question/explanation
        :valueType   :db.type/string
        :cardinality :db.cardinality/one
        :doc         "Markdown string to explain the answer."}

   #:db{:ident       :question/choices
        :valueType   :db.type/ref
        :cardinality :db.cardinality/many
        :isComponent true
        :doc         "Possible answers choice to the student."}

   #:db{:ident       :question/answers
        :valueType   :db.type/ref
        :cardinality :db.cardinality/many
        :isComponent true
        :doc         "Possible answers to the student."}

   #:db{:ident       :answer/value
        :valueType   :db.type/string
        :cardinality :db.cardinality/one
        :doc         "Answer's value."}

   #:db{:ident       :answer/position
        :valueType   :db.type/long
        :cardinality :db.cardinality/one
        :doc         "Specify the position when ordering possible choices."}

   #:db{:ident       :answer/kind
        :valueType   :db.type/ref
        :cardinality :db.cardinality/one
        :doc         "Answers kind."}

   #:db{:ident       :answer.kind/string}
   #:db{:ident       :answer.kind/numerical}

   #:db{:ident       :answer/documentation
        :valueType   :db.type/string
        :cardinality :db.cardinality/many
        :doc         "Documentation to the question for adding details."}])

(def finanche-clash-schema
  (into [] cat [tags-schema user-schema lecture-schema quiz-schema question-answer-schema]))


(def datomic-uri "datomic:free://localhost:4334/finance-clash")

(def cfg {:store {:backend :file
                  :path    "sql/finance-clash-datahike"}
          :schema-flexibility :write
          :keep-history?      true})

(def conn (atom (d/connect datomic-uri)))

(defn get-conn [] (d/connect datomic-uri))

(defn get-db
  ([] (get-db @conn))
  ([conn] (d/db conn)))

(set! *print-length* 250)

(defn init []
  (d/delete-database datomic-uri)
  (d/create-database datomic-uri)
  (let [connection (d/connect datomic-uri)]
    (d/transact connection finanche-clash-schema)
    (reset! conn connection)
    true))

(defn execute-query! [& rest])

(comment

  (init)

  (def conn (atom (d/connect datomic-uri)))

  (let [connection (d/connect datomic-uri)]
    (d/transact connection finanche-clash-schema))

  (->> (d/q '[:find ?e ?attr
              :in $
              :where
              [?e :db/ident ?attr]]
            (d/db (d/connect datomic-uri)))
       (into [])
       (sort-by first))

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

  finanche-clash-schema
  )

;; (comment

;;   (def cfg {:store              {:backend  :pg
;;                                  :username "postgres"
;;                                  :password "postgres"
;;                                  :path     "/datahike"
;;                                  :host     "localhost"
;;                                  :port     5432}
;;             :schema-flexibility :write
;;             :keep-history?      true})

;;   ;; The first transaction will be the schema we are using:
;;   (d/transact conn [{:db/ident       :name
;;                      :db/valueType   :db.type/string
;;                      :db/cardinality :db.cardinality/one }
;;                     {:db/ident       :age
;;                      :db/valueType   :db.type/long
;;                      :db/cardinality :db.cardinality/one }])

;;   ;; Let's add some data and wait for the transaction
;;   (d/transact conn [{:name "Alice", :age 20 }
;;                     {:name "Bob", :age 30 }
;;                     {:name "Charlie", :age 40 }
;;                     {:age 15 }])

;;   (d/q '[:find ?e ?n ?a
;;          :keys entity name age
;;          :where
;;          [?e :name ?n]
;;          [?e :age ?a]]
;;        @conn)

;;   (->> (d/q '[:find ?e ?attr
;;               :in $
;;               :where
;;               [?e :db/ident ?attr]]
;;             (d/db @conn))
;;        (into [])
;;        (sort-by second))

;;   #_(d/transact conn [{:db/ident       :name
;;                        :db/valueType   :db.type/string
;;                        :db/cardinality :db.cardinality/one }
;;                       {:db/ident       :age
;;                        :db/valueType   :db.type/long
;;                        :db/cardinality :db.cardinality/one }])



;;   )
