(ns finance-clash-web.chapter-selection.subs
  (:require
   [clojure.string :as str]
   [datascript.core :as d]
   [finance-clash-web.db :refer (empty-ds)]
   [finance-clash-web.subs :as subs]
   [re-frame.core :as rf :refer (reg-sub)]))

(defn parse-int-safe [s]
  (try
    (js/parseInt s)
    (catch js/Error _ 9999)))

(reg-sub
 ::chapter-available
 (fn [db _]
   (get-in db [:chapter-selection :available])))

(reg-sub
 ::chapter-priority
 (fn [db _]
   (get-in db [:chapter-selection :priority])))

(reg-sub
 ::ds
 (fn [db [_ ds-key]]
   (get-in db [:ds ds-key] empty-ds)))

(reg-sub
 ::chapters
 :<- [::ds :questions]
 (fn [ds]
   (->> :quiz/title
        (d/datoms ds :aevt)
        (map :e)
        (d/pull-many ds '[*])
        (sort-by #(-> % :quiz/title (str/split  #" " 2) first parse-int-safe)))))

(comment
  (rf/clear-subscription-cache!)
  @(rf/subscribe [::chapters])

  (rf/subscribe [::subs/ds :questions])
  (let [ds @(rf/subscribe [::ds :questions])]
    (d/q '[:find ?e
           :where
           [?e :quiz/title]]
     ds)
    )

  @(rf/subscribe [::chapter-available])
  )
