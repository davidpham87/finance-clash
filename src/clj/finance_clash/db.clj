(ns finance-clash.db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]))

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
create table quizz_attempts (
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
