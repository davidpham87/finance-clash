(ns finance-clash.core
  (:require
   [clojure.reflect :as r]
   [next.jdbc :as jdbc]
   [honeysql.core :as sql]
   [honeysql.helpers :as hsql]
   [finance-clash.db]))

(def ds (jdbc/get-datasource finance-clash.db/db))

(defn init! []
  (jdbc/execute! ds ["
create table user (
  id varchar(32),
  username varchar(32),
  password varchar(255)
)"])

  (jdbc/execute! ds ["
drop table user
"])

  (jdbc/execute! ds ["
drop table questions
"])

  (jdbc/execute! ds ["
create table questions (
  id varchar(10) PRIMARY KEY,
  chapter integer,
  number integer,
  correct_response integer,
  duration integer,
  availability boolean,
  difficulty varchar(20)
)"])

  (jdbc/execute! ds ["
create table quizz (
  question varchar(10),
  user varchar(32),
  attempt integer default 0,
  success boolean default false
) "])



  (jdbc/execute! ds ["
insert into quizz(question, user, attempt, success)
  values('0_5', '1',1, false)"])

  (jdbc/execute! ds ["
update quizz set attempt=0
where user = \"1\""])

  (jdbc/execute! ds ["
insert into user(id, username, password)
  values('1', 'David','david@davidolivier.pro')"])

  (jdbc/execute! ds ["
insert into user(id, username, password)
  values('2', 'vincent','vincent@davidolivier.pro')"])

  (jdbc/execute! ds ["
SELECT rowid, * FROM user;
"]))
