(ns kouyou.manage
  (:require
   [clojure.string]
   [kouyou.db.core :as db]
   [kouyou.layout :as layout]
   [ring.util.response :refer [redirect]]
   [struct.core :as st]))


(def board-schema
  {:nick 
   [st/string
    [st/required :message "your board needs a nickname -- like 'a' for anime"]
    {:message "that nickname is already used"
     :validate (fn [nick] (not (:exists (db/does-board-exist {:nick nick}))))}]
   :name [st/string
          [st/required :message "your board needs a name"]]})

(defn validate-board [params]
  (first (st/validate params board-schema)))

(defn clean-params [{tagline :tagline :as params}]
  (if (clojure.string/blank? tagline) (dissoc params :tagline) params))

(defn create-board! [{params :params}]
  (if-let [errors (validate-board params)]
    (-> (redirect "/manage/create-board")
        (assoc :flash (assoc params :errors errors)))
    (do (-> (clean-params params)
            (db/create-board!))
        (-> (redirect "/manage/create-board")
            (assoc :flash (assoc params :success true))))))
