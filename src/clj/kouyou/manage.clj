(ns kouyou.manage
  (:require
   [clojure.string]
   [kouyou.authentication :as auth]
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

(def staff-schema
  {:username
   [st/string
    [st/required :message "username required"]
    {:message "that username is already used"
     :validate (fn [username] (empty? (db/get-user {:username username})))}]
   :role
   [st/integer-str
    [st/required :message "role required"]
    {:message "invalid role"
     :validate (fn [id] (and (>= 3 id) (<= 0 id)))}]
   :password
   [st/string
    [st/required :message "password required"]]}) ;; also passwords don't match error

(defn validate-board [params]
  (first (st/validate params board-schema)))

(defn validate-staff [params]
  (first (st/validate params staff-schema)))

;; Try not to use as->
(defn clean-board-params [{:keys [tagline is_hidden is_text_only] :as params}]
  (as-> (if (clojure.string/blank? tagline) (dissoc params :tagline) params) params
    (if (nil? is_hidden) (assoc params :is_hidden false) (assoc params :is_hidden true))
    (if (nil? is_text_only) (assoc params :is_text_only false) (assoc params :is_text_only true))))

;; Try not to use as->
(defn clean-staff-params [{:keys [username role password] :as params}]
  (as-> (if (clojure.string/blank? username) (dissoc params :username) params)
        cleaned_params
    (if (clojure.string/blank? role)
      (dissoc cleaned_params :role)
      cleaned_params)
    (if (clojure.string/blank? password)
      (dissoc cleaned_params :password)
      cleaned_params)))

;; these functions are very similar. Candidate for refactor
;; Shouldn't we be cleaning before validating?
(defn create-board! [{params :params}]
  (if-let [errors (validate-board params)]
    (-> (redirect "/manage/create-board")
        (assoc :flash (assoc params :errors errors)))
    (do (-> (clean-board-params params)
            (db/create-board!))
        (-> (redirect "/manage/create-board")
            (assoc :flash (assoc params :success true))))))

(defn create-staff! [{params :params}]
  (if-let [errors (validate-staff params)]
    (-> (redirect "/manage/staff")
        (assoc :flash (assoc params :errors errors)))
    (do (-> (clean-staff-params params)
            (auth/create-user!)) ;; this function errors -- fix me
        (-> (redirect "/manage/staff")
            (assoc :flash (assoc params :success true))))))

;; this function is a garbage fire
(defn edit-board! [{{nick :nick} :path-params params :params }]
  (if-let [{:keys [nick id]} (db/get-board-by-nick {:nick nick})]
    (if-let [errors (validate-board params)]
    (-> (redirect (format "/manage/boards/%s" nick))
        (assoc :flash (assoc params :errors errors)))
    (do (-> (clean-board-params params)
            (merge {:id id})
            (db/update-board!))
        (-> (redirect (format "/manage/boards/%s" (:nick params)))
            (assoc :flash (assoc params :success true)))))
    (layout/error-page {:status 404, :title "404 - Page not found"})))

(defn delete-board! [{{nick :nick} :path-params}]
  (if (db/does-board-exist {:nick nick})
    (db/delete-board-by-nick! {:nick nick})
    (layout/error-page {:status 404, :title "404 - Page not found"})))
