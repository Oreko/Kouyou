(ns kouyou.boards
  (:require
   [kouyou.db.core :as db]
   [clojure.string]
   [struct.core :as st]))


(defn thread-list [board_id thread_count offset]
  {:threads (vec (db/get-board-threads-paginated {:id board_id
                                                  :count thread_count
                                                  :offset offset}))})

(defn get-teaser-posts [thread_id post_count]
  {:primary (db/get-primary-thread-post {:id thread_id})
   :posts (vec (db/get-last-nonprimary-posts-n {:id thread_id :count post_count}))})

(defn thread-teaser-wrapper [post_count {thread_id :id :as thread}]
  (merge
   {:information thread}
   (db/get-thread-post-count {:id thread_id})
   (get-teaser-posts thread_id post_count)))

(defn get-whole-thread [thread_id]
  {:information (db/get-thread thread_id)
   :primary (db/get-primary-thread-post thread_id)
   :posts (vec (db/get-non-primary-thread-posts thread_id))})

(defn create-primary! [params]
  (let [post_id (db/create-primary! params)]
    (merge post_id
           (db/get-primary-post-id-from-id post_id))))

;; refactor candidate. Something like for each key in [list]
(defn clean-params [{:keys [name email content subject media] :as params}]
  (as-> (if (clojure.string/blank? name) (dissoc params :name) params)
        clean_params
    (if (clojure.string/blank? email)
      (dissoc clean_params :email)
      clean_params)
    (if (clojure.string/blank? content)
      (dissoc clean_params :content)
      clean_params)
    (if (clojure.string/blank? subject)
      (dissoc clean_params :subject)
      clean_params)
    (if (or
         (clojure.string/blank? (:filename media))
         (= (:size media) 0))
      (dissoc clean_params :media)
      clean_params)))

(def post-schema
  [[:content
    st/string
    {:message "your post body was too short"
     :validate (fn [content] (>= (count content) 3))}]])

(defn validate_file_type [_] ;; todo
  (constantly true))

(def thread-schema
  [[:media
    st/map
    {:message "you need an image to start a thread"
     :validate (fn [{:keys [filename size content-type]}]
                 (and (not-empty filename)
                      (not= 0 size)
                      (validate_file_type content-type)))}]])

(defn validate-thread [params]
  (first (st/validate params 
                      (into [] (concat post-schema thread-schema)))))

(defn validate-post [params]
  (first (st/validate params post-schema)))

(defn check-and-bump! [email thread_id post_id]
  (when (not-any? true? [(= "sage" email)
                         (-> (db/get-thread thread_id) (:is_saged))])
    (db/bump-thread! {:thread_id (:id thread_id) :post_id (:id post_id)}))
  post_id)
