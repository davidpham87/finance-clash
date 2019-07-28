(ns finance-clash.specs.questions
  (:require
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as gen]
   [clojure.test.check.generators]))

(def difficulty->duration
  {:easy 60
   :medium 180
   :hard 300})

(def difficulty->attempts
  {:easy 1
   :medium 2
   :hard 2})

(s/def ::non-blank (s/and string? (complement clojure.string/blank?)))
(s/def ::question ::non-blank)
(s/def ::response ::non-blank)
(s/def ::responses (s/coll-of ::response :min-count 4 :max-count 6 :distinct true))
(s/def ::correct-response
  (s/or :double (s/double-in :min 0 :NaN? false :infinite? false)
        :int (s/int-in 0 10)))
(s/def ::duration (s/and int? pos?)) ;; time in seconds
(s/def ::difficulty #{:easy :medium :hard})
(s/def ::kind #{:single :multiple :numerical})

(s/def ::question-card
  (s/keys :req-un [::question ::responses ::kind
                   ::correct-response ::duration ::difficulty]))

(def sample-questions (gen/sample (s/gen ::question-card)))

(comment

  (gen/generate (s/gen ::question-card))
  (gen/generate (s/gen ::correct-response))

 (-> {:question "", :responses [ "q" "7" "J" "wi7" "cDCE" ], :correct-response 22, :duration 1, :difficulty :medium}
     clj->js)

 [{:question "How can an investor protect its stock position from a falling equity market?"
   :responses ["Eat, pray and love."
               "Enter long future positions on the CAC 40."
               "Sell targeted call options."
               "Buy the appropriate put options."]
   :correct-response 4
   :kind :single
   :duration (difficulty->duration :duration)
   :difficulty :easy}
  {:question "How can an investor protect its stock position from a falling equity market?"
   :responses ["Eat, pray and love."
               "Enter long future positions on the CAC 40."
               "Sell targeted call options."
               "Buy the appropriate put options."]
   :correct-response 4
   :kind :single
   :duration (difficulty->duration :hard)
   :difficulty :hard}
  {:question "What describes best the ECB?"
   :responses ["Eat, count and burn."
               "It stands for the European Commission on Bankruptcy."
               "An organization run by Christine Lagarde."
               "It is located in Berlin."]
   :correct-response 3
   :duration 30
   :kind :multiple
   :difficulty :easy}])
