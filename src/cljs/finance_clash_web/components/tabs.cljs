(ns finance-clash-web.component..tabs
  (:require
   [re-frame.core :refer [reg-event-db reg-sub path subscribe dispatch]]
   [goog.object :as gobj]

   [finance-clash-web.colors :as colors]
   [finance-clash-web.colors.mui-utils :refer [with-styles]]

   ["@material-ui/core/Tabs" :default mui-tabs]
   ["@material-ui/core/Tab" :default mui-tab]
   ["@material-ui/core/colors" :as mui-colors]
   ["@material-ui/core/styles" :refer [createMuiTheme]]))

(def default-id :default)

(reg-event-db
 ::set-tab
 (fn [db [_ [panel id tab]]] (assoc-in db [:ui-states ::tab [panel id]] tab)))

(reg-sub
 ::tab
 :<- [:ui-states]
 :<- [:active-panel]
 (fn [[m panel] [_ id]] (get-in m [::tab [panel (or id default-id)]])))

(defn get-tab
  "Helper function to retrieve the tab inside an event handler"
  [db active-panel]
  (get-in db [:ui-states ::tab [active-panel :default]] :overview))

(defn tab-style [theme]
  #js {:root #js {:color (colors/colors-rgb :citrine-dark)
                  :font-weight 600}
       :selected #js {:background-color "rgb(0,0,0)"
                      :color "white"}})

(defn tabs-container [tab tab-dispatcher {:keys [classes choices]}]
  [:> mui-tabs
   {:value (-> tab (or (.-value (first choices)))
               symbol str)
    :onChange tab-dispatcher
    :TabIndicatorProps {:style {:opacity 0}}
    :variant :fullWidth
    :centered true
    :style {:border-radius 0}}
   (for [m (js->clj choices :keywordize-keys true)]
     ^{:key (:value m)}
     [:> mui-tab (merge {:classes classes
                         :style {:padding 10}} m)])])

(defn tabs-comp [m]
  (let [tab (subscribe [::tab (-> m (:id default-id) keyword)])
        active-panel (subscribe [:active-panel])
        tab-dispatcher
        (fn [_ tab]
          (dispatch [::set-tab [@active-panel (-> m (:id default-id) keyword) (keyword tab)]]))]
    (fn [m]
      [tabs-container @tab tab-dispatcher m])))

(defn tabs [{:keys [choices id] :as m}]
  [:> (with-styles [tab-style] tabs-comp) m])
