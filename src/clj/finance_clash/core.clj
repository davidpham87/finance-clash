(ns finance-clash.core
  (:require
   [clojure.reflect :as r]
   [next.jdbc :as jdbc]
   [honeysql.core :as sql]
   [honeysql.helpers :as hsql]
   [finance-clash.db]))

#_(clojure.use '[next.jdbc :as jdbc])
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
  difficulty varchar(20)
)"])

  (jdbc/execute! ds ["
create table chapters (
  chapter integer,
  available boolean default false,
  priority boolean default false
) "])

  (jdbc/execute! ds ["
drop table quizz_attempt
"])

  (jdbc/execute! ds ["
drop table quizz
"])


  (jdbc/execute! ds ["
create table quizz_attempt (
  series integer,
  question varchar(10),
  user varchar(32),
  attempt integer default 0,
  success boolean default false
) "])

  (jdbc/execute! ds ["
create table quizz (
  series integer,
  question varchar(10)
)"])


  (jdbc/execute! ds ["
drop table quizz_series
"])

  (jdbc/execute! ds ["
create table quizz_series (
  id integer primary key autoincrement,
  release_date text
)"])

  (jdbc/execute! ds ["
drop table budget;"])

  (jdbc/execute! ds ["
create table budget (
  user varchar(32),
  wealth double,
  update_at text DEFAULT (datetime('now', 'utc'))
)"])

  (jdbc/execute! ds [" drop table budget_history;"])

  (jdbc/execute! ds ["
create table budget_history (
  user varchar(32),
  exchange_value double,
  update_at text DEFAULT (datetime('now', 'utc'))
)"])

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
