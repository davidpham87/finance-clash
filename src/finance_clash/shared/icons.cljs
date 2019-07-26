(ns finance-clash.shared.icons)

(defn ->ion-icon [name]
  (fn [m]
    (reagent/as-element [:> ion-icons {:name name :size 32 :color (.-tintColor m)}])))
