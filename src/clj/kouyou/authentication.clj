(ns kouyou.authentication
  (:require
   [buddy.auth :as auth]
   [buddy.hashers :as hashers]
   [kouyou.db.core :as db]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [redirect]]
   [struct.core :as st]))


;; add note about tuning parameters
(defn create-user! [name password role]
  (jdbc/with-transaction [t-conn db/*db*]
                         (if-not (empty? (db/get-user t-conn {:username name}))
                           (throw (ex-info "User already exists!"
                                           {:kouyou/error-id ::duplicate-user
                                            :error "User already exists!"}))
                           (let [verifier (hashers/derive password {:alg :argon2id})]
                             (db/create-user! t-conn
                                              {:username name
                                               :verifier verifier
                                               :role role})))))

(defn authenticate-user [name password]
  (when-let [{verifier :verifier :as user} (db/get-user {:username name})]
    (when (:valid (hashers/verify password verifier))
          (dissoc user :verifier :created_at))))

(def login-schema
  {:username
   [st/string
    [st/required :message "username required"]]
   :password
   [st/string
    [st/required :message "password required"]]})

(defn validate-login [params]
  (first (st/validate params login-schema)))

(defn login! [{:keys [params session]}]
  (if-let [errors (validate-login params)]
    (-> (redirect "/login")
        (assoc :flash (assoc params :errors errors)))
    (if-let [user (authenticate-user (:username params) (:password params))]
      (-> (redirect "/manage")
          (assoc :session (assoc session :identity user)))
      (-> (redirect "/login")
          (assoc :flash (assoc params :errors {:login 
                                               "incorrect username or password"}))))))
