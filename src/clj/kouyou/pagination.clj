(ns kouyou.pagination
  (:import java.lang.Math))

(defn posint-or-nil [input] ;; I should refactor this to use some sugar
  (let [normalized
        (if (string? input)
          (try (-> input Integer/parseUnsignedInt)
               (catch NumberFormatException _ nil))
          input)]
    (if (pos-int? normalized)
      normalized
      nil)))

(def default-paginate-params
  {:page 1})

(def env-params
  {:size 15}) ;; actually get from the env. Also don't we want this to be more general than just for threads?

(defn extract [request]
  (let [params (:params request)
        params (merge default-paginate-params params env-params)
        paginate (select-keys params [:page :size])]
    (assoc paginate :page (-> (:page paginate) (posint-or-nil)))))

(defn calculate-offset [current_page page_size]
  (* (dec current_page) page_size))

(defn generate-page-range [request element_count]
  (let [paginate_params (extract request)]
    (->> (:size paginate_params) 
         (/ element_count) 
         Math/ceil 
         int 
         range 
         (map inc))))

(defn create [request element_count]
  (let [{:keys [page size] :as paginate-params} (extract request)]
    (if page
      (let [prev_page (max (dec page) 1)
            next_page (inc page)
            page_range (generate-page-range paginate-params element_count)
            offset (calculate-offset page size)]
        (merge paginate-params
               {:prev_page prev_page
                :next_page next_page
                :page_range page_range
                :offset offset}))
      nil)))
