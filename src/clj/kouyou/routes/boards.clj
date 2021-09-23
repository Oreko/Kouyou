(ns kouyou.routes.boards
  (:require
   [kouyou.layout :as layout]
   [kouyou.boards :as boards]
   [kouyou.db.core :as db]
   [kouyou.middleware :as middleware]
   [reitit.coercion.spec]
   [ring.util.http-response :as response]
   [ring.util.response]))

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

(defn board-page [{{:keys [nick flash]} :path-params :as request}]
  (if-let [board (db/get-board-by-nick {:nick nick})]
    (layout/render
     request "board.html" (merge {:board_nick nick
                                  :board_name (:name board)
                                  :boards (vec (db/get-boards))
                                  :threads (as-> (boards/thread-list (:id board) 10) arg ;; Pull the numbers from the configuration
                                             (:threads arg)
                                             (map (partial boards/thread-teaser-wrapper 4) arg))}
                                 (select-keys flash [:name :email :subject :content :errors])))
    (layout/error-page {:status 404, :title "404 - Page not found"})))

;; We're calling too many queries here for the same information - refactor time
(defn thread-page [{{:keys [nick]} :path-params :keys [parameters] :as request}]
  (if-let* [board (db/get-board-by-nick {:nick nick})
            thread_id (db/get-thread-id-by-nick-post {:nick nick :post_id (-> parameters :path :id)})]
    (layout/render
     request "thread.html" {:board_nick nick
                            :board_name (:name board)
                            :boards (vec (db/get-boards)) ;; Pull out all the nav related args out into their own function
                            :thread (boards/get-whole-thread thread_id)})
    (layout/error-page {:status 404, :title "404 - Page not found"})))

(defn create-thread-and-primary! [{:keys [path-params params]}]
  (if-let [errors (boards/validate-post params)]
    (-> (response/found (format "/boards/%s" (:nick path-params)))
        (assoc :flash (assoc params :errors errors)))
    (if-let [thread_id (db/create-thread-on-nick! path-params)]
      ((->> (boards/clean-params params)
            (merge thread_id)
            (db/create-primary!))
     ;; upload media from primary post id (when query is not nil, upload media)
       (response/found (format "/boards/%s/res/%d" (:nick path-params) (:id thread_id))))
      (layout/error-page {:status 404, :title "404 - Page not found"}))))


(defn board-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/boards/:nick"
    ["" {:get board-page}]
    ["/res/:id" {:get {:coercion reitit.coercion.spec/coercion
                       :parameters {:path {:id int?}}
                       :handler thread-page}}] ;; nest here "post"
    ["/thread" {:post create-thread-and-primary!}]]])