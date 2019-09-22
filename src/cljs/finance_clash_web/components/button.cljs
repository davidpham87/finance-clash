(ns finance-clash-web.components.button
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as rf :refer [dispatch]]
   ["@material-ui/core/Tooltip" :default mui-tooltip]
   ["@material-ui/core/Button" :default mui-button]
   ["@material-ui/icons/NavigateBefore" :default ic-navigate-before]
   ["@material-ui/icons/NavigateNext" :default ic-navigate-next]
   ["@material-ui/icons/Send" :default ic-send]))

(defn previous-button [step event]
  [:> mui-tooltip {:title "Previous"}
   [:> mui-button {:disabled (zero? step)
                   :on-click #(dispatch event) :style {:margin-right 10}}
    [:> ic-navigate-before]]])

(defn next-button [step event]
  [:> mui-tooltip {:title "Next"}
   [:> mui-button {:disabled (= 2 step)
                   :color :primary :variant :contained
                   :on-click #(dispatch event)
                   :style {:margin-left 10 :margin-right 10}}
    [:> ic-navigate-next]]])

(defn submit-button
  ([event] [submit-button event {}])
  ([event {:keys [tooltip text] :or {tooltip "Submit" text nil}}]
   [:> mui-tooltip {:title tooltip}
    [:> mui-button
     {:color :secondary :variant :contained
      :style {:margin-left 10 :margin-right 10}
      :on-click #(when event (dispatch event))}
     [:> ic-send {:color :primary :style {:margin-right (if text 10 0)}}]
     (clojure.string/join " " [text])]]))
