(ns finance-clash-web.chapter-selection.core
  (:require
   [clojure.string :as s]
   [reagent.core :as reagent]
   [re-frame.core :as rf :refer (dispatch subscribe)]
   [finance-clash-web.components.mui-utils :refer (custom-theme)]
   [finance-clash-web.login.core :refer (root-panel)]
   ["@material-ui/core" :as mui]))

(def question-files
  ["0_Key_Notions.yaml"
   "1_Intro.yaml"
   "2_Etats_Financiers.yaml"
   "3_Le_Bilan.yaml"
   "4_Le_Compte_de_Resultat.yaml"
   "5_Introduction_Finance.yaml"
   "6_Le_Capital.yaml"
   "7_Cycles_Exploitation_Inv_Financement.yaml"
   "8_Comptabilite.yaml"
   "9_SIG_et_CAF.yaml"
   "10_Obligations.yaml"
   "11_Regulatory_Requirements.yaml"
   "12_Financial_Crisis.yaml"
   "13_TVA.yaml"
   "14_Diagnostic_Financier.yaml"
   "15_Financial_Risks_and_Options.yaml"
   "16_Couts_and_Comptabilite_Analytique.yaml"
   "17_Microeconomie.yaml"
   "18_Central_Banks.yaml"
   "19_Tresorerie.yaml"
   "20_empty.yaml"
   "21_Rating_agencies.yaml"
   "22_Credit_Talk.yaml"
   "23_Libor_Fwd_Rates.yaml"
   "24_Bourse.yaml"
   "25_Liquidity_Talk.yaml"])


(defn ->question-data
  [question]
  (let [x (-> question
              (s/split #"\\.")
              first
              (s/split #"_" 2))
        x (zipmap [:chapter :title] x)
        _ (println x)
        m (update (zipmap [:chapter :title] x) :title s/replace "_" " ")]
    m))
