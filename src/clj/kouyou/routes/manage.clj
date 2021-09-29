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
                        (select-keys flash [:nick :name :tagline :is_hidden :is_text_only :errors :success]))))

(defn edit-board-page [{flash :flash {nick :nick} :path-params :as request}]
  (if-let [board (db/get-board-by-nick {:nick nick})]
    (layout/render request "edit-board.html"
                   (merge {:boards (vec (db/get-boards))}
                          board
                          (select-keys flash [:errors :success])))
    (layout/error-page {:status 404, :title "404 - Page not found"})))


(defn manage-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/manage"
    [""
     {:get manage-page}]
    ["/create-board"
     {:get create-board-page
      :post manage/create-board!}]
    ["/edit-board/:nick"
     {:get edit-board-page
      :post manage/edit-board!}]
    ["/delete-board/:nick"
     {:post manage/delete-board!}]]])
