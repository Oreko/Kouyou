(ns kouyou.media
  (:require
   [kouyou.db.core :as db]
   [clojure.java.io :as io])
  (:import [java.io ByteArrayOutputStream]))


(defn validate-file [{:keys [filename size]}]
  (and
   (> size 0)
   (not= "" filename)))

(defn upload-file! [{:keys [tempfile filename content-type]} post_id]
  (with-open [in (io/input-stream tempfile)
              out (ByteArrayOutputStream.)]
    (io/copy in out)
    (db/store-media! {:type content-type
                      :data (.toByteArray out)
                      :name filename
                      :id post_id})
    post_id))
