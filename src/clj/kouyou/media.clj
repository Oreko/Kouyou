(ns kouyou.media
  (:require
   [kouyou.db.core :as db]
   [clojure.java.io :as io])
  (:import [java.awt.image AffineTransformOp BufferedImage]
           [java.io ByteArrayOutputStream]
           java.awt.geom.AffineTransform
           javax.imageio.ImageIO
           java.lang.Math))


(defn validate-file [{:keys [filename size]}]
  (and
   (> size 0)
   (not= "" filename)))

;; (create-thumbnail-filename [filename]
;;                            )

(defn upload-base-image![{:keys [tempfile filename size content-type]} {:keys [image_width image_height]} post_id]
  (with-open [in (io/input-stream tempfile)
              base_image_stream (ByteArrayOutputStream.)]
    (io/copy in base_image_stream)
    (db/store-media! {:type content-type
                      :data (.toByteArray base_image_stream)
                      :is_thumbnail false
                      :width image_width
                      :height image_height
                      :size size
                      :name filename
                      :id post_id})))

(defn upload_image_and_thumbnail! [{:keys [tempfile size filename] :as media}
                                   {:keys [thumb_width thumb_height]}
                                   post_id]
  (let [thumbnail_stream (ByteArrayOutputStream.)
        image (ImageIO/read tempfile)
        image_width (.getWidth image)
        image_height (.getHeight image)
        thumb_scale (min 1 (/ thumb_width image_width) (/ thumb_height image_height))
        thumbnail (if (= 1 thumb_scale)
                    image
                    (let [scale
                          (AffineTransform/getScaleInstance
                           (double thumb_scale) (double thumb_scale))]
                      (.filter (AffineTransformOp. scale AffineTransformOp/TYPE_BILINEAR)
                               image
                               (BufferedImage. (* thumb_scale image_width)
                                               (* thumb_scale image_height)
                                               (.getType image)))))]
    (ImageIO/write thumbnail "png" thumbnail_stream)
    (upload-base-image! media {:image_width image_width :image_height image_height} post_id)
    (db/store-media! {:type "image/png"
                      :data (.toByteArray thumbnail_stream)
                      :is_thumbnail true
                      :name filename
                      :id post_id
                      :size size ;; get real size of thumbnail
                      :width (-> (* thumb_scale image_width) Math/ceil int)
                      :height (-> (* thumb_scale image_height) Math/ceil int)})
    post_id))