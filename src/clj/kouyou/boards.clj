(ns kouyou.boards
  (:require
   [kouyou.db.core :as db]
   [clojure.string]
   [struct.core :as st]))


(defn thread-list [board_id thread_count]
  {:threads (vec (db/get-board-threads-n {:id board_id :count thread_count}))})

(defn get-teaser-posts [thread_id post_count]
  {:primary (db/get-primary-thread-post {:id thread_id})
   :posts (vec (db/get-last-nonprimary-posts-n {:id thread_id :count post_count}))})

(defn thread-teaser-wrapper [post_count {thread_id :id :as thread}]
  (merge
   {:information thread}
   (get-teaser-posts thread_id post_count)))

(defn get-whole-thread [thread_id]
  {:information (db/get-thread thread_id)
   :primary (db/get-primary-thread-post thread_id)
   :posts (vec (db/get-non-primary-thread-posts thread_id))})

(defn create-primary! [params]
  (let [post_id (db/create-primary! params)]
    {:id (:id post_id)
     :primary_id (:post_id (db/get-primary-post-id-from-id post_id))}))

(defn clean-params [{:keys [name email content subject media]}]
  (as-> (if (clojure.string/blank? name) {} {:name name}) params
    (if (clojure.string/blank? email) params (assoc params :email email))
    (if (clojure.string/blank? content) params (assoc params :content content))
    (if (clojure.string/blank? subject) params (assoc params :subject subject))
    (if (or
         (clojure.string/blank? (:filename media))
         (= (:size media) 0))
      params (assoc params :media media))))

(def post-schema
  [[:content
    st/required
    st/string
    {:message "your post body was too short"
     :validate (fn [content] (>= (count content) 3))}]])

(defn validate-post [params]
  (first (st/validate params post-schema)))

;; (defn upload-media [])
;; :media {:filename "", 
        ;; :content-type "application/octet-stream",
        ;; :tempfile #object[java.io.File 0x4d56d232 
        ;;         "/tmp/ring-multipart-2160933553900757697.tmp"], 
        ;; :size 0}
