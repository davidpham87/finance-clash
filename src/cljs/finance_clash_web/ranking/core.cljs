(ns finance-clash-web.ranking.core
  (:require
   ["@material-ui/core" :as mui]
   [finance-clash-web.components.mui-utils :refer
    [cs client-width with-styles text-field input-component panel-style]]
   [goog.object :as gobj]
   [reagent.core :as reagent]
   [finance-clash-web.ranking.events :as events]
   [finance-clash-web.ranking.subs :as subs]
   [re-frame.core :refer (dispatch subscribe)]))

(def table-header mui/TableHead)
(def table-row mui/TableRow)
(def table-cell mui/TableCell)
(def table-body mui/TableBody)

(defn icon [i n]
  (println i n)
  (cond
    (= i 1) "\uD83E\uDD47" ; gold medal
    (= i 2) "\uD83E\uDD48"
    (= i 3) "\uD83E\uDD49"
    (= i 4) "\uD83C\uDFC5"
    (= i 5) "	\uD83C\uDF96"
    (= i (- n 4)) "\uD83D\uDE48"
    (= i (- n 3)) "\uD83D\uDE48"
    (= i (- n 2)) "\uD83D\uDC22"
    (= i (- n 1)) "\uD83D\uDC0C"
    (= i n) "\uD83D\uDCA9"
    :else ""))

(defn cell
  ([s] (cell {} s))
  ([m s]
   [:> table-cell m s]))

(defn header []
  [:> table-header
   [:> table-row [cell "Rank"]
    [cell {:align :right} "Username"]
    [cell {:align :right} "Score"]]])

(defn rank-row [i username score n]
  [:> table-row
   [cell {:component :th :score :row} i ]
   [cell {:align :right} [:<> username " " [icon i n]]]
   [cell {:align :right} score]])

(defn body [ranking]
  (let [n (count ranking)]
    [:> table-body
     (for [[i {:keys [username wealth]}] (map-indexed vector ranking)]
       ^{:key username} [rank-row (inc i) username wealth n])]))

(defn content []
  (let [ranking @(subscribe [::subs/ranking])]
    [:> mui/Card {:elevation 10
                  :style {:min-width 480 :width "50vw"}}
     [:> mui/CardHeader {:title "Ranking"}]
     [:> mui/CardContent
      [:> mui/Table [header] [body ranking]]]]))


(defn root [{:keys [classes] :as props}]
  [:main {:class (cs (gobj/get classes "content"))
          :style {:min-height "100vh"
                  :background-image "url(images/portfolio.jpg)"
                  :background-position :center
                  :background-size :cover
                  :color :white
                  :z-index 0}}
   [:div {:class (cs (gobj/get classes "appBarSpacer"))}]
   [:div {:style {:min-height 480 :margin-top 5 :height "80%"
                  :display :flex :justify-content :center}}
    [:> mui/Fade {:in true :timeout 1000}
     [:div {:style {:margin :auto}}
      [content]]]]])

(defn init-events []
  (dispatch [::events/query-ranking]))

(defn root-panel [props]
  (init-events)
  [:> (with-styles [panel-style] root) props])
