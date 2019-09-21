(ns finance-clash.budget
  "Implements budget tables and routes"
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.set :refer (rename-keys)]
            [clojure.pprint :refer (pprint)]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [java-time :as jt]
            [finance-clash.db :refer (execute-query!)]
            [honeysql.core :as sql]
            [honeysql.helpers :as hsql
             :refer (select where from insert-into)]
            [muuntaja.core :as mc]
            [muuntaja.format.yaml :as yaml]
            [reitit.coercion.spec]
            [spec-tools.spec :as spec]))

(defn budget-tx
  "budget transaction query"
  [user-id value]
  (-> {:insert-into :budget_history}
      {:values [{:user user-id :exchange_value value}]}))

(defn budget
  ([user-id]
   (-> {:select [:*] :from [:budget] :where [:= :user user-id]}
       sql/format
       execute-query!))
  ([user-id v]
   (-> (hsql/update :budget)
       (hsql/sset {:wealth (sql/call :+ :wealth v)
                   :update_at (sql/call :now)})
       (where [:= :user user-id])
       sql/format
       execute-query!)))

(defn buy [user-id v]
  (budget user-id (- v)))

(defn earn [user-id v]
  (budget user-id v))
