(ns kouyou.routes.manage
  (:require
   [kouyou.db.core :as db]
   [kouyou.layout :as layout]
   [kouyou.manage :as manage]
   [kouyou.middleware :as middleware]))


(defn manage-page [request]
  (layout/render request "manage.html" {:boards (vec (db/get-boards))}))

(defn create-board-page [{flash :flash :as request}]
  (layout/render request "create-board.html"
                 (merge {:boards (vec (db/get-boards))}
                        (select-keys flash [:nick :name :tagline :errors :success]))))


(defn manage-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/manage"
    [""
     {:get manage-page}]
    ["/create-board"
     {:get create-board-page
      :post manage/create-board!}]]])
