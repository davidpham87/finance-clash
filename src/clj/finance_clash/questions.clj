(ns finance-clash.questions
  (:require [clojure.spec.alpha :as s]
            [spec-tools.spec :as spec]
            [reitit.coercion.spec]
            [muuntaja.core :as mc]
            [muuntaja.format.yaml :as yaml]
            [clj-http.client :as client]
            [clojure.string :as str]))


(def mi
  (mc/create (-> mc/default-options
                 (mc/install yaml/format))))

(def question-files
  ["0_Key_Notions.yaml"
   "1_Intro.yaml"
   "2_Etats_Financiers.yaml"
   "3_Le_Bilan.yaml"])

(defn read-questions [chapter]
  (->> (get question-files chapter)
       (str "questions/") clojure.java.io/resource slurp
       (mc/decode mi "application/x-yaml")))

(defn convert-question [chapter]
  (let [new-filename (-> (get question-files chapter)
                          str/lower-case
                          (str/replace #"yaml$" "json")
                          (as-> s (clojure.core/str "assets/questions/" s)))
        data (->> (read-questions chapter)
                  (mc/encode mi "application/json")
                  slurp)]
    (println new-filename)
    (spit new-filename data)))

(s/def ::chapter spec/int?)

(def routes
  ["/question"
   {:get {:coercion reitit.coercion.spec/coercion
          :summary "Retrieve questions for a given chapter."
          :parameters {:query (s/keys :req-un [::chapter])}
          :handler
          (fn [{{{:keys [chapter]} :query} :parameters :as m}]
            (let [questions (read-questions chapter)]
              {:status 200 :body questions}))}}])

(comment
  (-> (client/get "http://localhost:3000") :body)
  (->> (client/get "http://localhost:8050/question?chapter=0")
       :body
       (mc/decode mi "application/json"))
  (-> (client/get "http://localhost:3001/echo") :body)
  (doseq [chapter (range 4)]
    (convert-question chapter)))
