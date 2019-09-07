(ns finance-clash.core
  (:require
   [clojure.reflect :as r]
   [next.jdbc :as jdbc]

   [hugsql.core :as hugsql]
   [finance-clash.db]))

(def ds (jdbc/get-datasource finance-clash.db/db))

(defn init! []
  (jdbc/execute! ds ["
create table user (
  name varchar(32),
  email varchar(255)
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
