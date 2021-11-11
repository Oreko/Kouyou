(ns kouyou.routes.manage
  (:require
   [kouyou.authentication :as auth]
   [kouyou.db.core :as db]
   [kouyou.layout :as layout]
   [kouyou.manage :as manage]
   [kouyou.middleware :as middleware]))


(defn login-page [{flash :flash :as request}]
  (layout/render request "login.html" 
                 (select-keys flash [:username :errors])))

(defn manage-page [request]
  (layout/render request "manage.html"))

(defn create-board-page [{flash :flash :as request}]
  (layout/render request "create-board.html"
                 (select-keys flash [:nick :name :tagline :is_hidden :is_text_only :errors :success])))

(defn edit-board-page [{flash :flash {nick :nick} :path-params :as request}]
  (if-let [board (db/get-board-by-nick {:nick nick})]
    (layout/render request "edit-board.html"
                   (merge board
                          (select-keys flash [:errors :success])))
    (layout/error-page {:status 404, :title "404 - Page not found"})))

(defn staff-page [{flash :flash :as request}]
  (let [staff (db/get-staff)]
    (layout/render request "staff.html" 
                   (merge {:staff staff}
                          (select-keys flash [:username :errors :success])))))

(defn manage-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/login"
    {:get login-page
     :post auth/login!}]
   ["/manage"
    {:middleware [middleware/wrap-restricted]}
    [""
     {:get manage-page}]
    ["/create-board"
     {:get create-board-page
      :post manage/create-board!}]
    ["/boards/:nick"
     {:get edit-board-page
      :post manage/edit-board!}]
    ["/delete-board/:nick" ;; should we merge these two?
     {:post manage/delete-board!}]
    ["/staff"
     {:get staff-page
      :post manage/create-staff!}]
    ;; ["/staff/:id"
    ;;  {:get edit-staff-page
    ;;   :post manage/edit-staff!}]
    ]])
