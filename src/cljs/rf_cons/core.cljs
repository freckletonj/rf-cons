(ns rf-cons.core
  (:import goog.History)
  (:require
   ;; general stuff
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]

   ;; routing stuff
   [secretary.core :as secretary]
   [goog.events :as events]
   [goog.history.EventType :as EventType]

   ;; HTTP stuff
   [ajax.core :refer [GET PUT]]
   [clojure.walk :refer [keywordize-keys]])
  
  (:require-macros [reagent.ratom :refer [reaction]]
                   [secretary.core :refer [defroute]]))


;; config.cljs ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def debug?
  ^boolean js/goog.DEBUG)

(when debug?
  (enable-console-print!))


;; db.cljs ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def default-db
  {:current-user  {}
   
   :user-form-name "a"
   :user-form-count (rand-int 1000)
   
   :user-loading? false
   :user-upserting? false})


;; handlers.cljs ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def api-url "http://localhost:3449")


(re-frame/register-handler
 :set-active-panel
 (fn [db [_ arg]] (assoc db :active-panel arg)))

(re-frame/register-handler
 :change-user-form-count
 (fn [db [_ arg]] (assoc db :user-form-count arg)))

(re-frame/register-handler
 :change-user-form-name
 (fn [db [_ arg]] (assoc db :user-form-name arg)))

(re-frame/register-handler
 :change-current-user
 (fn [db [_ & args]]
   (GET (str api-url "/" (name (nth args 0)))
        {:handler       #(re-frame/dispatch [:process-change-current-user-response %1])
         :error-handler #(re-frame/dispatch [:error-change-current-user-response %1])})
   (assoc db :user-loading? true)))
(re-frame/register-handler
 :process-change-current-user-response
 (fn [db [_ & args]]
   (-> db
       (assoc :user-loading? false)
       (assoc :current-user (keywordize-keys (get (nth args 0) "body"))))))
(re-frame/register-handler
 :error-change-current-user-response
 (fn [db [_ _]]
   (assoc db :user-loading? false)))

(re-frame/register-handler
 :upsert-user
 (fn [db [_ & args]]
   (PUT (str api-url "/" (nth args 0) "/" (nth args 1))
        {:handler       #(re-frame/dispatch [:process-upsert-user-response %1])
         :error-handler #(re-frame/dispatch [:error-upsert-user-response %1])})
   (assoc db :user-upserting? true)))
(re-frame/register-handler
 :process-upsert-user-response
 (fn [db [_ & args]]
   (-> db
       (assoc :user-upserting? false)
       (assoc :current-user (keywordize-keys (get (nth args 0) "body"))))))
(re-frame/register-handler
 :error-upsert-response
 (fn [db [_ _]]
   (assoc db :user-upserting? false)))

(re-frame/register-handler
 :initialize-db
 (fn [db [_ _]]
   default-db))

(defn gen-reframe-handlers
  "register handlers"
  [handlers]
  (map (fn [[name f]]
         (re-frame/register-handler
          name f))
       handlers))




;; subs.cljs ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def sub-vars [:current-user
               :user-form-name
               :user-form-count
               :user-loading?
               :user-upserting?
               :active-panel])

(defn gen-reframe-subs
  "register re-frame subscriptions"
  [vars]
  (map (fn [name]
         (re-frame/register-sub
          name
          (fn [db] (reaction (name @db)))))
       vars))
(doall (gen-reframe-subs sub-vars)) ;; doall forces lazy evaluation


;; views.cljs ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; home
(defn home-panel []
  
  (let [current-user    (re-frame/subscribe [:current-user])
        user-form-name  (re-frame/subscribe [:user-form-name])
        user-form-count (re-frame/subscribe [:user-form-count])
        user-loading? (re-frame/subscribe [:user-loading?])
        user-upserting? (re-frame/subscribe [:user-upserting?])]
    (fn []
      [:div
       [:h1 "A simple template for Re-Frame"]
       [:p (str "current name is: " (:name @current-user))]
       [:p (str "current count is: " (:count @current-user))]
       [:button {:on-click #(re-frame/dispatch [:change-current-user :a])} "A"]
       [:button {:on-click #(re-frame/dispatch [:change-current-user :b])} "B"]
       [:button {:on-click #(re-frame/dispatch [:change-current-user :c])} "C"]
       [:div ;; TODO I could probably add a monad to wrap the context of the inputs to the button?
        [:input {:type      "text"
                 :value     @user-form-name
                 :on-change #(re-frame/dispatch [:change-user-form-name (-> % .-target .-value)])}]
        [:input {:type      "text"
                 :value     @user-form-count
                 :on-change #(re-frame/dispatch [:change-user-form-count (-> % .-target .-value)])}]
        [:button {:on-click #(re-frame/dispatch [:upsert-user
                                                 @user-form-name
                                                 @user-form-count])}
         "Upsert"]]
       (when @user-loading?
         [:p "Loading User"])
       (when @user-upserting?
         [:p "Uploading User"])

       ])))

;; main
(defmulti panels identity)
(defmethod panels :home-panel [] [home-panel])
(defmethod panels :default [] [:div [:p "loading..."]])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])]
    (fn []
      (panels @active-panel))))



;; routes.cljs ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defn app-routes []
  (secretary/set-config! :prefix "#")
  ;; --------------------
  ;; define routes here
  (defroute "/" []
    (re-frame/dispatch [:set-active-panel :home-panel]))

  ;; --------------------
  (hook-browser-navigation!))




;; core.cljs (original) ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(when debug?
  (println "dev mode"))

(defn mount-root []
  (reagent/render [main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init [] 
  (app-routes)
  (re-frame/dispatch-sync [:initialize-db])
  (mount-root))
