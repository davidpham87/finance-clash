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
      {:values [{:user user-id :exchange_value value}]}
      )

  )

(defn budget
  ([user-id])
  ([user-id v]))
