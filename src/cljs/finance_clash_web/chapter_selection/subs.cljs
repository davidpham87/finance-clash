(ns finance-clash-web.chapter-selection.subs
  (:require
   [datascript.core :as d]
   [finance-clash-web.subs :as subs]
   [finance-clash-web.db :refer (empty-ds)]
   [re-frame.core :as rf :refer (reg-sub)]))

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
        (sort-by :quiz/title))))

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
