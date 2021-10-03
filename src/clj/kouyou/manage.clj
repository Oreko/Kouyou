(ns kouyou.manage
  (:require
   [clojure.string]
   [kouyou.db.core :as db]
   [kouyou.layout :as layout]
   [ring.util.response :refer [redirect]]
   [struct.core :as st]))


;; todo Allow users to edit the board keeping the name the same 
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

(defn clean-params [{:keys [tagline is_hidden is_text_only] :as params}]
  (as-> (if (clojure.string/blank? tagline) (dissoc params :tagline) params) params
    (if (nil? is_hidden) (assoc params :is_hidden false) (assoc params :is_hidden true))
    (if (nil? is_text_only) (assoc params :is_text_only false) (assoc params :is_text_only true))))

;; these functions are very similar. Candidate for refactor
(defn create-board! [{params :params}]
  (if-let [errors (validate-board params)]
    (-> (redirect "/manage/create-board")
        (assoc :flash (assoc params :errors errors)))
    (do (-> (clean-params params)
            (db/create-board!))
        (-> (redirect "/manage/create-board")
            (assoc :flash (assoc params :success true))))))

;; this function is a garbage fire
(defn edit-board! [{{nick :nick} :path-params params :params }]
  (if-let [{:keys [nick id]} (db/get-board-by-nick {:nick nick})]
    (if-let [errors (validate-board params)]
    (-> (redirect (format "/manage/edit-board/%s" nick))
        (assoc :flash (assoc params :errors errors)))
    (do (-> (clean-params params)
            (merge {:id id}) 
            (db/update-board!))
        (-> (redirect (format "/manage/edit-board/%s" (:nick params)))
            (assoc :flash (assoc params :success true)))))
    (layout/error-page {:status 404, :title "404 - Page not found"})))

(defn delete-board! [{{nick :nick} :path-params}]
  (if (db/does-board-exist {:nick nick})
    (db/delete-board-by-nick! {:nick nick})
    (layout/error-page {:status 404, :title "404 - Page not found"})))
