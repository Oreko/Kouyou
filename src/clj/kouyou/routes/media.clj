(ns kouyou.routes.media
  (:require
   [clojure.java.io :as io]
   [kouyou.layout :as layout]
   [kouyou.db.core :as db]
   [kouyou.middleware :as middleware]
   [reitit.coercion.spec]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.exception :as exception]
   [ring.util.http-response :as response]))


(defn get-file [{{{:keys [id]} :path} :parameters}]
  (if-let [{:keys [data type]} (db/get-media {:id id})]
    (-> (io/input-stream data)
        (response/ok)
        (response/content-type type))
    (response/not-found)))

(def coercion-middleware
  (exception/create-exception-middleware
   (merge
    exception/default-handlers
    {:reitit.coercion/request-coercion
     (constantly (layout/error-page {:status 400, :title "400 - Bad Request"}))})))


(defn media-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/media/:id" {:get get-file
                   :coercion reitit.coercion.spec/coercion
                   :parameters {:path {:id pos-int?}}
                   :middleware [coercion-middleware
                                coercion/coerce-request-middleware]}]])
