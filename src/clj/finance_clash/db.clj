(ns finance-clash.db
  (:require [hugsql.core :as hugsql]))

(def db
  {:dbtype "sqlite"
   :dbname "sql/finance-clash-database.db"})

(defn init! []
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


  (jdbc/execute! ds ["
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
