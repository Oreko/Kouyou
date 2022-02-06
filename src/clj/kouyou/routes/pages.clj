(ns kouyou.routes.home
  (:require
   [clojure.java.io :as io]
   [kouyou.db.core :as db]
   [kouyou.layout :as layout]
   [kouyou.middleware :as middleware]
   [ring.util.response]))


(defn catalog-page [request]
  (layout/render request "pages-catalog.html" {:pages ()}))

(defn pages-renderer [request]
  (layout/render request "pages.html" {:markdown (-> "markdown/rules.md" io/resource slurp)}))


(defn home-routes []
  ["/pages"
   {:middleware [middleware/wrap-formats]}
   [""
    {:get catalog-page}]
   ["/:page-name"
    {:get pages-renderer}]])
