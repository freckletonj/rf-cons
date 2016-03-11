(ns rf-cons.core
    (:require [compojure.core :refer [GET PUT defroutes]]
              [ring.util.response :refer [file-response response]]
              [ring.middleware.reload :refer [wrap-reload]]
              [ring.adapter.jetty :as ring]
              [ring.middleware.json :refer [wrap-json-response]]
              [monger.core :as mg]
              [monger.collection :as mc]
              [monger.conversion :refer [from-db-object]]))

;; MongoDB ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; TODO: Reader Monad - this is how context should be passed around
;; http://software-ninja-ninja.blogspot.co.il/2014/04/5-faces-of-dependency-injection-in.html

(defn k-format [dmap & args]
  "format a list down to a string according to a map (data)
if any argument is a key in the map, return the corresponding
value"
  (apply str (map (fn [arg] (let [val (dmap arg)]
                              (if val val arg))) args)))

(def mongo-creds {:user "masteruser"
                  :pass "pass"
                  :host "localhost"
                  :port "27017"
                  :db   "pipegen"})

#_(def mongo-uri (k-format mongo-creds "mongodb://" :user ":" :pass "@" :host ":" :port "/" :db))
(def mongo-uri (k-format mongo-creds "mongodb://"  :host ":" :port "/" :db))

(def conn (atom (mg/connect-via-uri mongo-uri)))


;; counts resource ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
#_(def counts (atom {:a {:count 1}
                   :b {:count 2}
                   :c {:count 3}}))

;; counts controllers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def mongo-collection "testcollection")
(defn get-count [name] (mc/find-maps (:db @conn) mongo-collection {:name name}))
(defn update-count [name count] (mc/update (:db @conn) mongo-collection {:name name} {:name name :count count} {:upsert true}))


;; Handlers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defroutes handler

  ;; frontend SPA ----------
  (GET "/" [] (file-response "index.html" {:root "resources/public"}))

  
  ;; backend ----------
  (GET "/:name" [name]
       (let [ret {:body (dissoc (nth (from-db-object (get-count name) true #_"true keywordizes it") 0) :_id)}]
         (response ret)))
  
  (PUT "/:name/:value" [name value]
       (dosync
        (update-count name value)
        (response {:body {:name name :count value}})))) ;; should this return the db's response?


(def reloady-handler
  (-> #'handler
      wrap-reload
      wrap-json-response)
  #_(wrap-json-response (wrap-reload #'handler)))

