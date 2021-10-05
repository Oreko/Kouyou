(ns kouyou.routes.boards
  (:require
   [kouyou.boards :as boards]
   [kouyou.db.core :as db]
   [kouyou.layout :as layout]
   [kouyou.media :as media]
   [kouyou.middleware :as middleware]
   [kouyou.pagination :as pagination]
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

(defn board-page [{{nick :nick} :path-params flash :flash :as request}]
  (if-let [board (db/get-board-by-nick {:nick nick})]
    (if-let [pagination_map (pagination/create request (:total_thread_count (db/get-board-thread-count board)))]
        (layout/render
         request "board.html" (merge {:board_nick nick
                                      :board_name (:name board)
                                      :threads (as-> (boards/thread-list (:id board) (:size pagination_map) (:offset pagination_map)) arg ;; Pull the numbers from the configuration
                                                 (:threads arg)
                                                 (map (partial boards/thread-teaser-wrapper 4) arg))}
                                     {:pagination pagination_map}
                                     (select-keys flash [:name :email :subject :content :errors])))
        (layout/error-page {:status 400, :title "400 - Bad Request"}))
    (layout/error-page {:status 404, :title "404 - Page not found"})))

;; We're calling too many queries here for the same information - refactor time
(defn thread-page [{{nick :nick} :path-params :keys [flash parameters] :as request}]
  (if-let* [{:keys [name]} (db/get-board-by-nick {:nick nick})
            thread (db/get-thread-by-nick-primary {:nick nick :post_id (-> parameters :path :id)})]
    (layout/render
     request "thread.html" (merge {:board_nick nick
                                   :board_name name
                                   :thread (boards/get-whole-thread thread)}
                                  (select-keys flash [:name :email :subject :content :errors])))
    (layout/error-page {:status 404, :title "404 - Page not found"})))

(defn create-thread-and-primary! [{{:keys [nick]} :path-params
                                   :keys [params]}]
  (let [clean_params (boards/clean-params params)]
    (if-let [errors (boards/validate-thread clean_params)]
      (-> (redirect (format "/boards/%s" nick))
          (assoc :flash (assoc clean_params :errors errors)))
      (if-let [thread_id (db/create-thread-on-nick! {:nick nick})]
        (let [{:keys [id primary_post_id]} (-> clean_params
                                               (merge thread_id)
                                               (boards/create-primary!))]
          (when (media/validate-file (:media params))
            (media/upload_image_and_thumbnail! (:media params) {:thumb_width 250 :thumb_height 250} id))
          (redirect (format "/boards/%s/res/%s" nick primary_post_id)))
        (layout/error-page {:status 404, :title "404 - Page not found"})))))

(defn create-reply! [{{:keys [nick]} :path-params
                      :keys [params parameters]}]
  (let [clean_params (boards/clean-params params)]
    (if-let [errors (boards/validate-post clean_params)]
      (-> (redirect (format "/boards/%s/res/%d" nick (-> parameters :path :id)))
          (assoc :flash (assoc clean_params :errors errors)))
      (if-let [thread_id (db/get-thread-id-by-nick-post {:nick nick :post_id (-> parameters :path :id)})]
        (let [post_id (->> (boards/clean-params clean_params)
                           (merge thread_id)
                           (db/create-reply!)
                           (boards/check-and-bump! (:email clean_params) thread_id))]
          (when (media/validate-file (:media clean_params))
            (media/upload_image_and_thumbnail! (:media clean_params) {:thumb_width 250 :thumb_height 250} (:id post_id)))
          (redirect (str "/boards/" nick)))
        (layout/error-page {:status 404, :title "404 - Page not found"})))))

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
    [""
     {:get board-page
      :post create-thread-and-primary!}]
    ["/res/:id"
     {:coercion reitit.coercion.spec/coercion
      :parameters {:path {:id pos-int?}}
      :middleware [;;coercion-middleware ;; currently does not play nicely with the dev error handling
                   coercion/coerce-request-middleware]}
     [""
      {:get thread-page
       :post create-reply!}]]]])
