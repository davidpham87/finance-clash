(ns finance-clash-web.components.mui-utils
  (:require
   ["@material-ui/core/InputBase" :default mui-input-base]
   ["@material-ui/core/ListItem" :default mui-list-item-raw]
   ["@material-ui/core/ListItemIcon" :default mui-list-item-icon]
   ["@material-ui/core/ListItemText" :default mui-list-item-text]
   ["@material-ui/core/MenuItem" :default mui-menu-item]
   ["@material-ui/core/Select" :default mui-select]
   ["@material-ui/core/TextField" :default mui-text-field]
   ["@material-ui/core/Tooltip" :default mui-tooltip]
   ["@material-ui/core/styles" :refer [withStyles createMuiTheme]]
   [clojure.string :as str]
   [finance-clash-web.components.colors :as colors]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [reagent.dom :as dom]
   [reagent.impl.template :as rtpl]))

(defn cs [& names]
  (str/join " " (filter identity names)))

(defn client-width [component]
  (let [node (dom/dom-node component)]
    (if node (.-clientWidth node) 0)))

(defn combine-style-fns [style-fns]
  (fn [theme]
    (reduce #(.assign js/Object %1 %2) ((apply juxt style-fns) theme))))

(defn with-styles [style-fns component]
  ((withStyles (combine-style-fns style-fns)) (reagent/reactify-component component)))

(defn adapt-mui-component-style [style mui-component]
  ((withStyles style) mui-component))

(defn mui-list-item
  [[icon text] {:keys [dispatch-event style tooltip?] :or {tooltip? false}}]
  (let [list-item
        [:> mui-list-item-raw
         {:button true
          :on-click (when dispatch-event #(rf/dispatch dispatch-event))
          :style style}
         (when icon
           [:> mui-list-item-icon {:style {:padding-left "8px"}}
            [:> icon {:style {:color "white"}}]])
         [:> mui-list-item-text
          {:primaryTypographyProps #js {:style #js {:color "white"}}
           :primary text}]]]
    (if tooltip?
      [:> mui-tooltip {:title text :placement :bottom-start} list-item]
      list-item)))

(defn select [{:keys [subscription-vector]}]
  (let [value (rf/subscribe subscription-vector)]
    (fn [{:keys [event-vector style label on-change choices disabled? default-value]
          :or {disabled? false} :as m}]
      [:> mui-select
       {:value (or @value default-value)
        :disabled disabled?
        :label label
        :on-change
        (or on-change
            (fn [event]
              (rf/dispatch (conj event-vector (keyword (.. event -target -value))))))
        :style style}
        (for [c choices]
          ^{:key (:id c)}
          [:> mui-menu-item {:value (:id c nil)
                             :name (:label c nil)} (:label c "None")])])))

(def input-component
  (reagent/reactify-component
    (fn [props]
      [:input (-> props
                  (assoc :ref (:inputRef props))
                  (dissoc :inputRef))])))

(def textarea-component
  (reagent/reactify-component
    (fn [props]
      [:textarea (-> props
                     (assoc :ref (:inputRef props))
                     (dissoc :inputRef))])))

(defn text-field
  "Textfield in mui have a few deficiencies and interop with reagent is not
  optimal (for example the cursors always goes at the end.) This function
  corrects this.

  Additionally, adds the ability to use mui/InputBase as element."
  [props & children]
  (let [mui-text-component (if (:input-base props) mui-input-base mui-text-field)
        props
        (-> props
            (assoc-in [:InputProps :inputComponent]
                      (cond
                        (and (:multiline props) (:rows props)
                             (not (:maxRows props)))
                        textarea-component

                        (:multiline props)
                        nil
                        ;; Select doesn't require cursor fix so default can be used.
                        (:select props)
                        nil

                        :else
                        input-component))
            rtpl/convert-prop-value)]
    (apply reagent/create-element mui-text-component props
           (map reagent/as-element children))))

(def custom-theme
  (createMuiTheme
   #js {:palette #js {:primary #js {:main (colors/colors-rgb :graphite)}
                      :secondary #js {:main (colors/colors-rgb :coral-dark) :dark "#ca0"}
                      :type "light"
                      :background #js {:default (colors/colors-rgb :sand-bright)}}
        :typography #js
        {:fontFamily #js ["Helvetica"]
         :h1 #js {:fontFamily #js ["Roboto" "Helvetica"]}
         :h5 #js {:fontFamily #js ["Roboto" "Helvetica"]}
         :h6 #js {:fontFamily #js ["Roboto" "Helvetica"]}}}))

(def drawer-width 280)

(defn panel-style [theme]
  (let [drawer-width 240
        transitions (.-transitions theme)
        duration (.-duration transitions)
        easing-sharpe (.. transitions -easing -sharp)]
    #js
    {:appBarSpacer (.. theme -mixins -toolbar)
     :content #js {:flex-grow 1
                   :padding (.spacing theme  3)
                   :height "100vh"
                   :overflow "auto"
                   :background-color (.. theme -palette -background -default)}}))
