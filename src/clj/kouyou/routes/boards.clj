(ns kouyou.routes.boards
  (:require
   [kouyou.layout :as layout]
   [kouyou.boards :as boards]
   [kouyou.media :as media]
   [kouyou.db.core :as db]
   [kouyou.middleware :as middleware]
   [reitit.coercion.spec]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.exception :as exception]
   [ring.util.response :refer [redirect]]))

;; https://clojuredocs.org/clojure.core/if-let#example-5797f83ce4b0bafd3e2a04b9
(defmacro if-let*
  ([bindings then]
   `(if-let* ~bindings ~then nil))
  ([bindings then else]
   (if (seq bindings)
     `(if-let [~(first bindings) ~(second bindings)]
        (if-let* ~(drop 2 bindings) ~then ~else)
        ~else)
     then)))

(defn board-page [{{:keys [nick]} :path-params :keys [flash] :as request}]
  (if-let [board (db/get-board-by-nick {:nick nick})]
    (layout/render
     request "board.html" (merge {:board_nick nick
                                  :board_name (:name board)
                                  :postform_action (format "/boards/%s/thread" nick)
                                  :boards (vec (db/get-boards))
                                  :threads (as-> (boards/thread-list (:id board) 10) arg ;; Pull the numbers from the configuration
                                             (:threads arg)
                                             (map (partial boards/thread-teaser-wrapper 4) arg))}
                                 (select-keys flash [:name :email :subject :content :errors])))
    (layout/error-page {:status 404, :title "404 - Page not found"})))

;; We're calling too many queries here for the same information - refactor time
(defn thread-page [{{:keys [nick]} :path-params :keys [flash parameters] :as request}]
  (if-let* [{:keys [name]} (db/get-board-by-nick {:nick nick})
            thread_id (db/get-thread-id-by-nick-post {:nick nick :post_id (-> parameters :path :id)})]
    (layout/render
     request "thread.html" (merge {:board_nick nick
                                   :board_name name
                                   :postform_action (format "/boards/%s/res/%d/post" nick (-> parameters :path :id))
                                   :boards (vec (db/get-boards)) ;; Pull out all the nav related args out into their own function
                                   :thread (boards/get-whole-thread thread_id)}
                                  (select-keys flash [:name :email :subject :content :errors])))
    (layout/error-page {:status 404, :title "404 - Page not found"})))

(defn create-thread-and-primary! [{{:keys [nick]} :path-params 
                                   :keys [params]}]
  (if-let [errors (boards/validate-thread params)]
    (-> (redirect (format "/boards/%s" nick))
        (assoc :flash (assoc params :errors errors)))
    (if-let [thread_id (db/create-thread-on-nick! {:nick nick})]
      (let [{:keys [id primary_id]} (->> (boards/clean-params params)
                              (merge thread_id)
                              (boards/create-primary!))]
        (when (media/validate-file (:media params))
         (media/upload_image_and_thumbnail! (:media params) {:thumb_width 250 :thumb_height 250} id))
        (redirect (format "/boards/%s/res/%s" nick primary_id)))
      (layout/error-page {:status 404, :title "404 - Page not found"}))))

(defn create-reply! [{{:keys [nick]} :path-params
                      :keys [params parameters]}]
  (if-let [errors (boards/validate-post params)]
    (-> (redirect (format "/boards/%s/res/%d" nick (-> parameters :path :id)))
        (assoc :flash (assoc params :errors errors)))
    (if-let [thread_id (db/get-thread-id-by-nick-post {:nick nick :post_id (-> parameters :path :id)})]
      (let [post_id (->> (boards/clean-params params)
               (merge thread_id)
               (db/create-reply!)
               (boards/check-and-bump! (:email params) thread_id))]
          (when (media/validate-file (:media params))
            (media/upload_image_and_thumbnail! (:media params) {:thumb_width 250 :thumb_height 250} (:id post_id)))
          (redirect (str "/boards/" nick)))
      (layout/error-page {:status 404, :title "404 - Page not found"}))))

(def coercion-middleware
  (exception/create-exception-middleware
   (merge
    exception/default-handlers
    {:reitit.coercion/request-coercion
     (constantly (layout/error-page {:status 400, :title "400 - Bad Request"}))})))


(defn board-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/boards/:nick"
    ["" {:get board-page}]
    ["/res/:id"
      {:coercion reitit.coercion.spec/coercion
       :parameters {:path {:id int?}}
       :middleware [;coercion-middleware ;; currently does not play nicely with the dev error handling
                    coercion/coerce-request-middleware]}
      ["" {:get thread-page}]
      ["/post" {:post create-reply!}]]
     ["/thread" {:post create-thread-and-primary!}]]])