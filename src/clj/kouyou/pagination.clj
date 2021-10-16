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
  {:size 15
   :limit 10}) ;; actually get from the env. Also don't we want this to be more general than just for threads?

(defn extract [request]
  (let [params (:params request)
        params (merge default-paginate-params params env-params)
        paginate (select-keys params [:page :size :limit])]
    (assoc paginate :page (-> (:page paginate) (posint-or-nil)))))
;; Are we always sure that the user will give us positive values for env-params?
;; This function should be renamed

(defn take-neighborhood [limit center values]
  (let [bottom (->> (/ limit 2) 
                    Math/floor 
                    int 
                    (- center)
                    (min (- (count values) limit))
                    (max 0))]
    (->> values
         (drop bottom) 
         (take limit))))
;; Bug! We give too few pages in back when at the left of the array
;; e.g. if we have 100 pages, then at page 100 we give [96 97 98 99 100]
;; instead of 9 pages behind. It's obvious why given the code above, so fix it.

(defn calculate-offset [current_page page_size]
  (* (dec current_page) page_size))

(defn generate-page-range [{:keys [limit page size]} element_count]
  (->> size
       (/ element_count)
       Math/ceil
       int
       range
       (map inc)
       (take-neighborhood limit page))) ;; Should filter before map as it's cheaper

(defn create [request element_count]
  (let [{:keys [page size] :as paginate-params} (extract request)]
    (if page
      (let [prev_page (max (dec page) 1)
            next_page (inc page) ;; both should nil if outside range actually
            page_range (generate-page-range paginate-params element_count)
            offset (calculate-offset page size)]
        (merge paginate-params
               {:prev_page prev_page
                :next_page next_page
                :page_range page_range
                :offset offset}))
      nil)))
