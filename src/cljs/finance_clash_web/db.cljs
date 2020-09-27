(ns finance-clash-web.db
  (:require
   [cljs.reader]
   [clojure.string :as str]
   [datascript.core :as d]
   [re-frame.core :as rf :refer (reg-cofx)]))

;; TODO get this into a config file
(def question-files
  ["0_key_notions.json"
   "1_intro.json"
   "2_etats_financiers.json"
   "3_le_bilan.json"
   "4_le_compte_de_resultat.json"
   "5_introduction_finance.json"
   "6_le_capital.json"
   "7_cycles_exploitation_inv_financement.json"
   "8_comptabilite.json"
   "9_sig_et_caf.json"
   "10_obligations.json"
   "11_regulatory_requirements.json"
   "12_financial_crisis.json"
   "13_tva.json"
   "14_diagnostic_financier.json"
   "15_financial_risks_and_options.json"
   "16_couts_and_comptabilite_analytique.json"
   "17_microeconomie.json"
   "18_central_banks.json"
   "19_tresorerie.json"
   "20_empty.json"
   "21_rating_agencies.json"
   "22_credit_talk.json"
   "23_libor_fwd_rates.json"
   "24_bourse.json"
   "25_liquidity_talk.json"])

;; TODO get this into a config file
(def chapter-names
  {"9" "Sig et Caf",
   "3" "Le Bilan",
   "22" "Credit Talk",
   "4" "Le Compte de Resultat",
   "8" "Comptabilité",
   "14" "Diagnostic Financier",
   "21" "Rating Agencies",
   "20" "Empty",
   "19" "Trésorerie",
   "17" "Microeconomie",
   "25" "Liquidity Talk",
   "15" "Financial Risks and Options",
   "7" "Cycles Exploitation Inv Financement",
   "5" "Introduction Finance",
   "18" "Central Banks",
   "12" "Financial Crisis",
   "13" "Tva",
   "24" "Bourse",
   "6" "Le Capital",
   "1" "Intro",
   "0" "Key Notions",
   "11" "Regulatory Requirements",
   "2" "Etats Financiers",
   "16" "Couts and Comptabilite Analytique",
   "10" "Obligations",
   "23" "Libor Fwd Rates"})

(def super-users #{"admin" "neo2551"})

(def empty-ds
  (d/empty-db {:datomic.db/id {:db/unique :db.unique/identity}
               :quiz/questions {:db/type :db.type/ref
                                :db/cardinality :db.cardinality/many}
               :question/choices {:db/type :db.type/ref
                                  :db/cardinality :db.cardinality/many}
               :question/tags {:db/type :db.type/ref
                               :db/cardinality :db.cardinality/many}
               :question/answers {:db/type :db.type/ref
                                  :db/cardinality :db.cardinality/many}}))

(def default-db
  {:active-panel :login
   :panel-props {} ;; hack to dispatch arguments to component in lazy mode
   :credentials {}
   :question-files question-files
   :chapter-names chapter-names
   :ds {:questions empty-ds
        :chapters empty-ds} ;; datascript store
   :user {:id "1"} ;; data for auth
   :user-input {}
   :loading {}
   :errors {}
   :help-event [:set-panel :welcome]
   :quiz-question {} ;; storing the displayed quiz question
   :series-questions {:medium #{} :hard #{} :easy #{}}
   :series-questions-seen #{}
   :series-questions-answered #{}
   :super-users super-users
   :ui-states
   {:drawer-open? false
    :drawer-displayed-sublists #{}}})

(def fcw-user-key "finance-clash-web-user")  ;; localstore key

(defn set-user-ls
  "Puts user into localStorage"
  [user]
  (.setItem js/localStorage fcw-user-key (str user))) ;; sorted-map written as an EDN map

;; Removes user information from localStorge when a user logs out.
(defn remove-user-ls
  "Removes user from localStorage"
  []
  (.removeItem js/localStorage fcw-user-key))

(defn set-ls
  "Puts a key into localStorage"
  [k v]
  (.setItem js/localStorage k (clj->js v)))

(defn remove-ls
  [k]
  (.removeItem js/localStorage k ))

;; -- cofx Registrations  -----------------------------------------------------
;;
;; To see it used, look in `events.cljs` at the event handler for `:initialise-db`.
;; That event handler has the interceptor `(inject-cofx :local-store-user)`
;; The function registered below will be used to fulfill that request.
;;
;; We must supply a `sorted-map` but in localStorage it is stored as a `map`.
(reg-cofx
 :local-store-user
 (fn [cofx _]
   (assoc cofx :local-store-user
          (into (sorted-map)
                (some->> (.getItem js/localStorage fcw-user-key)
                         (cljs.reader/read-string))))))


(reg-cofx
 :local-store
 (fn [cofx local-store-key]
   (assoc cofx :local-store
          (into (sorted-map)
                (some->> (.getItem js/localStorage local-store-key)
                         (cljs.reader/read-string))))))
