(ns kouyou.handler
  (:require
   [kouyou.middleware :as middleware]
   [kouyou.layout :refer [error-page]]
   [kouyou.routes.boards :refer [board-routes]]
   [kouyou.routes.home :refer [home-routes]]
   [kouyou.routes.manage :refer [manage-routes]]
   [kouyou.routes.media :refer [media-routes]]
   [reitit.ring :as ring]
   [kouyou.env :refer [defaults]]
   [mount.core :as mount]))

(mount/defstate init-app
  :start ((or (:init defaults) (fn [])))
  :stop  ((or (:stop defaults) (fn []))))

(mount/defstate app-routes
  :start
  (ring/ring-handler
   (ring/router
    [(home-routes)
     (board-routes)
     (media-routes)
     (manage-routes)])
   (ring/routes
    (ring/redirect-trailing-slash-handler {:method :strip})
    (ring/create-resource-handler
     {:path "/"})
    (ring/create-default-handler
     {:not-found
      (constantly (error-page {:status 404, :title "404 - Page not found"}))
      :method-not-allowed
      (constantly (error-page {:status 405, :title "405 - Not allowed"}))
      :not-acceptable
      (constantly (error-page {:status 406, :title "406 - Not acceptable"}))}))))

(defn app []
  (middleware/wrap-base #'app-routes))
