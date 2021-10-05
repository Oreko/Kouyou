(ns kouyou.layout
  (:require
   [clojure.java.io]
   [kouyou.db.core :as db]
   [markdown.core]
   [ring.util.http-response :refer [content-type ok]]
   [ring.util.anti-forgery :refer [anti-forgery-field]]
   [ring.util.response]
   [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
   [selmer.parser :as parser]
   [selmer.filters :as filters])
  (:import java.lang.Math))


(parser/set-resource-path!  (clojure.java.io/resource "html"))
(parser/add-tag! :csrf-field (fn [_ _] (anti-forgery-field)))
(filters/add-filter! :markdown (fn [content] [:safe (markdown.core/md-to-html-string content)]))
(filters/add-filter! :empty? empty?)
(filters/add-filter! :to-kb (fn [bytes] (-> (/ bytes 1024) Math/ceil int)))
(filters/add-filter! :second second)

(defn render
  "renders the HTML template located relative to resources/html"
  [request template & [params]]
  (content-type
    (ok
      (parser/render-file
        template
        (assoc params
          :page template
          :boards (vec (db/get-boards))
          :csrf-token *anti-forgery-token*)))
    "text/html; charset=utf-8"))

(defn error-page
  "error-details should be a map containing the following keys:
   :status - error status
   :title - error title (optional)
   :message - detailed error message (optional)

   returns a response map with the error page as the body
   and the status specified by the status key"
  [error-details]
  {:status  (:status error-details)
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body    (parser/render-file "error.html" error-details)})
