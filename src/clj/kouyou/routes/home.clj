(ns kouyou.routes.home
  (:require
   [clojure.java.io :as io]
   [kouyou.layout :as layout]
   [kouyou.middleware :as middleware]
   [ring.util.response]))


(defn index-page [request]
  (layout/render request "index.html" {:markdown (-> "markdown/index.md" io/resource slurp)}))

(defn rules-page [request]
  (layout/render request "pages.html" {:markdown (-> "markdown/rules.md" io/resource slurp)}))


(defn home-routes []
  ["" 
   {:middleware [middleware/wrap-formats]}
   ["/" {:get index-page}]
   ["/rules" {:get rules-page}]])

