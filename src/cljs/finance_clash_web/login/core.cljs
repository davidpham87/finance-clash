(ns finance-clash-web.login.core
  (:require-macros [reagent.ratom])
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as rf
    :refer [reg-sub reg-fx reg-event-fx dispatch
            reg-event-db subscribe]]
   [goog.object :as gobj]

   [finance-clash-web.components.colors :as colors]
   [finance-clash-web.components.mui-utils :refer
    [cs client-width with-styles text-field input-component panel-style]]

   ["@material-ui/core" :as mui]
   ["@material-ui/core/Tabs" :default mui-tabs]
   ["@material-ui/core/Tab" :default mui-tab]
   ["@material-ui/core/Typography" :default mui-typography]
   ["@material-ui/core/InputLabel" :default mui-input-label]
   ["@material-ui/core/Button" :default mui-button]
   ["@material-ui/icons/Lock" :default ic-lock]
   ["@material-ui/icons/PersonAdd" :default ic-person-add]))

(reg-event-db
 ::set-tab
 (fn [db [_ [panel tab]]] (assoc-in db [:ui-states ::tab panel] tab)))

(reg-sub
 ::tab
 :<- [:ui-states]
 :<- [:active-panel]
 (fn [[m panel]] (get-in m [::tab panel])))

(reg-event-db
 ::record-email
 (fn [db [_ credentials]]
   (assoc-in db [:credentials :email] (:email credentials))))

(reg-event-fx
 ::submit-login-credentials
 (fn [{db :db} [_ credentials]]
   {:db (assoc db :credentials credentials)
    :dispatch-n [[:user/login credentials]]}))

(reg-event-fx
 ::submit-register-credentials
 (fn [{db :db} [_ credentials]]
   {:db (assoc db :credentials credentials)
    :dispatch-n [[:user/register credentials]]}))

(reg-event-fx
 ::submit-update-userprofile
 (fn [{db :db} [_ credentials]]
   {:db (assoc db :credentials credentials)
    :dispatch-n [[:user/update-userprofile credentials]]}))

(reg-sub
 ::login-credentials
 (fn [db _] (db :credentials)))

(reg-event-db
 ::clear-login-credentials
 (fn [db _] (assoc db :credentials {:email "" :password ""})))

(def panel-font-color :white)

(defn tabs [tab tab-dispatcher user-logged?]
  [:> mui-tabs
   {:value (if user-logged? "update-userprofile"
               (-> tab (or :sign-in) symbol str))
    :onChange tab-dispatcher
    :variant :fullWidth
    :style {:width 480}}

   (when-not user-logged?
     [:> mui-tab
      {:label "Sign in" :value "sign-in"}])
   (when-not user-logged?
     [:> mui-tab
      {:label "Sign up" :value "register"}])
   #_(when-not user-logged?
     [:> mui/Tab
      {:label "Reset password" :value "reset-password"}])

   (when user-logged?
     [:> mui-tab
      {:label "Update User Profile" :value "update-userprofile"}])])

(defn tabs-comp []
  (let [tab (subscribe [::tab])
        active-panel (subscribe [:active-panel])
        user-logged? (subscribe [:user-logged?])
        tab-dispatcher
        (fn [_ tab]
          (dispatch [::set-tab [@active-panel (keyword tab)]]))]
    [tabs @tab tab-dispatcher @user-logged?]))

(defn log [& args]
  (apply js/console.log args))

(defn login-style [theme]
  (let [spacing (fn [x] ((.. theme -spacing) x))]
    #js {:paper #js {:color :white
                     :marginTop (spacing 2)
                     :width 400
                     :display "flex"
                     :flexDirection "column"
                     :alignItems "center"
                     :padding (str (spacing 2) "px "
                                   (spacing 2) "px "
                                   (spacing 3) "px "
                                   (spacing 3) "px")}
         :avatar #js {:margin (spacing 1)
                      :backgroundColor (colors/colors-hex :secondary)
                      :color :white}
         :form #js {:width "100%"
                    :marginTop (spacing 1)}
         :submit #js {:marginTop (spacing 2)}}))

(defn form-title [s]
  [:> mui/Typography {:component "h5" :variant "h5"
                      :style {:color panel-font-color
                              :font-weight :bold
                              :font-family "Helvetica"}} s])
(defn input-label [m s]
  [:> mui/InputLabel (merge {:style {:color "grey" :z-index 1 :padding-left 10
                                     :padding-right 10}} m) s])

(defn logout-button [classes]
  [:> mui/Button
   {:fullWidth true
    :variant "contained"
    :color :secondary
    :onClick #(rf/dispatch [:user/logout])
    :class (cs (gobj/get classes "submit"))}
   "Logout"])

(defn email
  [{:keys [credentials update-credentials loading? errors disabled?]}]
  (let [error? (some #(= (:status %) 404) @errors)]
    [:> mui/FormControl {:margin "normal" :required true
                         :fullWidth true :error error?}
     [input-label {:html-for "email"} "Email Address"]
     [:> mui/Input
      {:id "email" :name "email" :type "email"
       :inputComponent input-component
       :autoComplete "email" :autoFocus true
       :error error?
       :disabled (or @loading? (when disabled? @disabled?))
       :inputProps {:style {:background-color :white :padding-left 10
                            :padding-right 10}}
       :value (or (@credentials :email) "")
       :on-change (partial update-credentials :email)}]]))

(defn username [{:keys [credentials update-credentials loading? required?]}]
  [:> mui/FormControl
   {:margin "normal" :required (if (nil? required?) true required?)
    :color :white :fullWidth true}
   [input-label {:html-for "username"} "Username"]
   [:> mui/Input
    {:id "username" :name "username"
     :autoComplete "username"
     :inputComponent input-component
     :value (or (@credentials :username) "")
     :disabled @loading?
     :style {:background-color :white :padding-left 10 :padding-right 10}
     :on-change (partial update-credentials :username)}]])

(defn password
  [{:keys [credentials update-credentials loading? errors required?]}]
  (let [error? (some #(or (= (:status %) 404)
                          (= (:type %) :insecure-password)) @errors)]
    [:> mui/FormControl
     {:margin "normal" :required (if (nil? required?) true required?)
      :color :white :fullWidth true :error error?}
     [input-label {:html-for "password"} "Password"]
     [:> mui/Input
      {:id "password" :name "password" :type "password"
       :autoComplete "current-password"
       :value (or (@credentials :password) "")
       :error error?
       :disabled @loading?
       :style {:background-color :white :padding-left 10 :padding-right 10}
       :on-change (partial update-credentials :password)}]
     (when (some #(= (:status %) 404) @errors)
       [:> mui/FormHelperText "Invalid credentials. Please enter them again."])
     (when (some #(= (:type %) :insecure-password) @errors)
       [:> mui/FormHelperText "Insecure password. Password should be at least 12 characters."])]))

(defn confirm-password
  [{:keys [credentials update-credentials loading? errors required?]}]
  [:> mui/FormControl
   {:margin "normal"
    :required (if (nil? required?) true required?)
    :fullWidth true
    :error (not (zero? (count @errors)))}
   [input-label {:html-for "confirm-password"} "Confirm password"]
   [:> mui/Input
    {:id "confirm-password" :name "confirm-password" :type "password"
     :value (or (@credentials :confirm-password) "")
     :error (some #(= (:type %) :unequal-password) @errors)
     :disabled @loading?
     :style {:background-color :white :padding-left 10 :padding-right 10}
     :on-change (partial update-credentials :confirm-password)}]
   (when (some #(= (:type %) :unequal-password) @errors)
     [:> mui/FormHelperText "Passwords are not equal. Please enter the password again."])])

(defn submit-button [{:keys [loading?]} classes button-label]
  [:> mui/Button
   {:type "submit" :fullWidth true :variant "contained"
    :disabled @loading?
    :color "secondary" :class (cs (gobj/get classes "submit"))}
   button-label])

(defn new-crendentials []
  (reagent/atom {:email "" :password "" :remember-me true}))

(defn sign-in-form-inner
  [{:keys [classes form-controls panel-font-color]
    :or {panel-font-color panel-font-color}}]
  [:form {:on-submit
          (fn [e]
            (.preventDefault e)
            (rf/dispatch [::submit-login-credentials @(:credentials form-controls)]))
          :class (cs (gobj/get classes "form"))}
   [email form-controls]
   [password form-controls]
   [submit-button form-controls classes "Sign in"]
   [:> mui/Grid {:container true :justify :space-between
                 :alignItems :center
                 :style {:margin-top 10}}
    [:> mui/Grid {:item true :xs 6}
     [:> mui/FormControlLabel
      {:control
       (reagent/as-element
        [:> mui/Switch
         {:on-change #(swap! (:credentials form-controls) assoc :remember-me
                             (-> % .-target .-checked))
          :checked (-> form-controls :credentials deref :remember-me)
          :disabled @(:loading? form-controls)
          :value "remember" :color "secondary"}])
       :label (reagent/as-element
               [:> mui/Typography {:style {:color panel-font-color}}
                "Remember me"])}]]

    #_[:> mui/Grid {:item true :xs 4}
     [:> mui/Link
      {:component :button
       :style {:color panel-font-color}
       :onClick
       (fn [e]
         (.preventDefault e)
         (rf/dispatch [::record-email @(:credentials form-controls)])
         (dispatch [::set-tab [@(subscribe [:active-panel])
                               :reset-password]]))}
      "Forgot password?"]]]])

(defn sign-in-form [classes]
  (let [credentials (new-crendentials)
        event-value #(-> % .-target .-value)
        update-credentials #(swap! credentials assoc %1 (event-value %2))
        loading? (subscribe [:loading :uesr/login])
        errors (subscribe [:errors :user/login])
        form-controls {:credentials credentials
                       :update-credentials update-credentials
                       :loading? loading?
                       :errors errors}]
    (swap! (:credentials form-controls)
           merge @(subscribe [::login-credentials]))
    (fn [classes]
      [sign-in-form-inner {:classes classes :form-controls form-controls}])))

(defn password-valid? [event-id credentials]
  (dispatch [:clear-error event-id])
  (let [m credentials
        equal-password-inputs? (= (:password m) (:confirm-password m))
        secured-password? (> (count (:password m)) 11)]
    (when-not equal-password-inputs?
      (dispatch [:user/register-validation-error
                 :unequal-password
                 "Password are not identical"]))
    (when-not secured-password?
      (dispatch [:user/register-validation-error
                 :insecure-password
                 "Password should be at least 12 characters long."]))
    (and equal-password-inputs? secured-password?)))

(defn reset-password-form [classes]
  (let [credentials (reagent/atom {:email "" :password "" :remember-me false})
        event-value #(-> % .-target .-value)
        update-credentials #(swap! credentials assoc %1 (event-value %2))
        loading? (subscribe [:loading :uesr/login])
        errors (subscribe [:errors :user/login])
        form-controls {:credentials credentials
                       :update-credentials update-credentials
                       :loading? loading?
                       :errors errors}]
    (swap! (:credentials form-controls) merge @(subscribe [::login-credentials]))
    (fn [classes]
      [:form {:on-submit
              (fn [e]
                (.preventDefault e)
                (rf/dispatch [::submit-login-credentials @credentials]))
              :class (cs (gobj/get classes "form"))}
       [email form-controls]
       [submit-button form-controls classes "Reset Password"]])))

(defn register-form-inner
  [{:keys [classes form-controls panel-font-color]
    :or {panel-font-color panel-font-color}}]
  [:form {:on-submit
          (fn [e]
            (.preventDefault e)
            (when-let [errors? (or (password-valid? :user/register @(:credentials form-controls)))]
              (rf/dispatch [::submit-register-credentials @(:credentials form-controls)])))
          :class (cs (gobj/get classes "form"))}
   [email form-controls]
   [password form-controls]
   [:> mui/Typography {:style {:color panel-font-color}}
    "Passwords contain at least 12 characters."]
   [confirm-password form-controls]
   [submit-button form-controls classes "Sign up"]])

(defn register-form [classes]
  (let [credentials (new-crendentials)
        event-value #(-> % .-target .-value)
        update-credentials #(swap! credentials assoc %1 (event-value %2))
        loading? (subscribe [:loading :uesr/register])
        errors (subscribe [:errors :user/register])
        form-controls {:credentials credentials
                       :update-credentials update-credentials
                       :loading? loading?
                       :errors errors}]
    (swap! (:credentials form-controls) merge @(subscribe [::login-credentials]))
    (fn [classes]
      [register-form-inner
       {:classes classes :form-controls form-controls
        :panel-font-color panel-font-color}])))

(defn update-userprofile-form-inner
  [{:keys [classes form-controls panel-font-color]
    :or {panel-font-color panel-font-color}}]
  [:form {:on-submit (:on-submit-fn form-controls)
          :class (cs (gobj/get classes "form"))}
   [email (assoc form-controls :disabled? (reagent/atom true))] ;; hack to disable
   [username form-controls]
   [password (assoc form-controls :required? false)]
   [:> mui/Typography {:style {:color panel-font-color}}
    "Passwords contain at least 12 characters."]
   [confirm-password (assoc form-controls :required? false)]
   [submit-button form-controls classes "Udpate profile"]])

(defn update-userprofile-form [classes]
  (let [credentials (new-crendentials)
        event-value #(-> % .-target .-value)
        update-credentials #(swap! credentials assoc %1 (event-value %2))
        loading? (subscribe [:loading :uesr/update-userprofile])
        errors (subscribe [:errors :user/update-userprofile])
        on-submit-fn
        (fn [e]
          (.preventDefault e)
          (when-let
              [errors?
               (or (zero? (count (:password @credentials)))
                   (password-valid? :user/update-userprofile
                                    @credentials))]
            (rf/dispatch [::submit-update-userprofile @credentials])))
        form-controls {:credentials credentials
                       :update-credentials update-credentials
                       :loading? loading?
                       :errors errors
                       :on-submit-fn on-submit-fn}]

    (swap! (:credentials form-controls) merge @(subscribe [:user-profile]))
    (fn [classes]
      ^{:key :update-userprofile}
      [update-userprofile-form-inner
       {:classes classes :form-controls form-controls
        :panel-font-color panel-font-color}])))

(defmulti content (fn [& args] (first args)) :default :sign-in)

(defn content-style [paper-class]
  {:elevation 0
   :class paper-class
   :style {:background-color "rgba(0,0,0,0)"
           :color :white}})

(defmethod content :sign-in
  [_ classes]
  [:> mui/Paper (content-style (cs (gobj/get classes "paper")))
   [:> mui/Avatar {:class (cs (gobj/get classes "avatar"))}
    [:> ic-lock]]
   [form-title "Login"]
   [sign-in-form classes]])

(defmethod content :register
  [_ classes]
  [:> mui/Paper (content-style (cs (gobj/get classes "paper")))
   [:> mui/Avatar {:class (cs (gobj/get classes "avatar"))}
    [:> ic-person-add]]
   [form-title "Sign up"]
   [register-form classes]])

(defmethod content :reset-password
  [_ classes]
  [:> mui/Paper (content-style (cs (gobj/get classes "paper")))
   [:> mui/Avatar {:class (cs (gobj/get classes "avatar"))}
    [:> ic-person-add]] ;; TOOD change icon
   [form-title "Reset password"]
   [reset-password-form classes]])

(defmethod content :update-userprofile
  [_ classes]
  [:> mui/Paper (content-style (cs (gobj/get classes "paper")))
   [:> mui/Avatar {:class (cs (gobj/get classes "avatar"))}
    [:> ic-person-add]]
   [form-title "User Profile"]
   [update-userprofile-form classes]
   [logout-button classes]])

(defn evolution-system-image []
  [:div
   {:style {:display :block
            :position :absolute
            :right 0
            :bottom 0
            :background-image "url(images/background.jpeg)"
            :background-size "100%"
            :background-color :transparent
            :background-position-x :right
            :background-position-y :bottom
            :background-repeat :no-repeat
            :z-index -1
            :align-self :flex-end
            :margin-top 16
            :height 220
            :width 485}}])

(defn init-events []
  (rf/dispatch [::clear-login-credentials]))

(defn root [m]
  (let [tab (rf/subscribe [::tab])
        user-logged? (rf/subscribe [:user-logged?])]
    (fn [{:keys [classes] :as props}]
      ;; we want a different login value whenever the user is logged.
      ;; this complects the component quite a bit though. Is it a good choice?
      (let [tab-value (if @user-logged? :update-userprofile @tab)]
        [:main {:class (cs (gobj/get classes "content"))
                :style {:background-image "url(images/background.jpeg)"
                        :background-position :center
                        :color :white
                        :z-index 0}}
         [:div {:class (cs (gobj/get classes "appBarSpacer"))}]
         #_[evolution-system-image]
         [:> mui/Fade {:in true :timeout 1000}
          [:> mui/Grid {:container true :justify "center"}
           [:> mui/Paper {:elevation 0
                          :style {:margin-top "10vh"
                                  :background-position :center
                                  :background-color "rgba(0,0,0,0)"
                                  :color "white"
                                  :z-index 10}}
            [tabs-comp]
            [:> mui/Grid {:container true :justify "center"}
             ^{:key tab-value}
             [content tab-value classes]]]]]]))))

(defn root-panel [props]
  (init-events)
  [:> (with-styles [panel-style login-style] root) props])

(comment
  (reagent/render [root-panel] (.getElementById js/document "app"))

  (rf/dispatch [:initialise-db])
  (ocio.core/main)
  (rf/dispatch [::submit-login-credentials {:email "d@vescore.com" :password "d"}])

  (rf/dispatch [::submit-login-credentials {:email "asdf@asdf.com" :password "asdf"}])
  (rf/dispatch [::submit-login-credentials {:email "d@vescore.com" :password "d"}])

  (reagent/render [ocio.views/app] (.getElementById js/document "app")))
