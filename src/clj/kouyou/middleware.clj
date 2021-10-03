(ns kouyou.middleware
  (:require
   [buddy.auth.middleware :refer [wrap-authentication]]
   [buddy.auth.backends.session :refer [session-backend]]
   [buddy.auth.accessrules :refer [restrict]]
   [buddy.auth :refer [authenticated?]]
   [clojure.tools.logging :as log]
   [kouyou.config :refer [env]]
   [kouyou.env :refer [defaults]]
   [kouyou.layout :refer [error-page]]
   [kouyou.middleware.formats :as formats]
   [muuntaja.middleware :refer [wrap-format wrap-params]]
   [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
   [ring-ttl-session.core :refer [ttl-memory-store]]
   [ring.middleware.defaults :refer [site-defaults wrap-defaults]]))


(defn auth-on-error [request _]
  (error-page
   {:status 403
    :title "Invalid Authorization"
    :message (str "Access to " (:uri request) " is not authorized. Please login.")}))

(defn wrap-restricted [handler]
  (restrict handler {:handler authenticated?
                     :on-error auth-on-error}))

(defn wrap-internal-error [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable t
        (log/error t (.getMessage t))
        (error-page {:status 500
                     :title "Something very bad has happened!"
                     :message "We've dispatched a team of highly trained gnomes to take care of the problem."})))))

(defn wrap-csrf [handler]
  (wrap-anti-forgery
    handler
    {:error-response
     (error-page
       {:status 403
        :title "Invalid anti-forgery token"})}))

(defn wrap-formats [handler]
  (let [wrapped (-> handler wrap-params (wrap-format formats/instance))]
    (fn [request]
      ;; disable wrap-formats for websockets
      ;; since they're not compatible with this middleware
      ((if (:websocket? request) handler wrapped) request))))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      (wrap-authentication (session-backend))
      (wrap-defaults
       (-> site-defaults
           (assoc-in [:security :anti-forgery] false)
           (assoc-in  [:session :store] (ttl-memory-store (* 60 30)))))
      wrap-internal-error))
