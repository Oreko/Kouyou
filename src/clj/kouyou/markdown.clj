(ns kouyou.markdown
  [:require
   [clojure.string :as str]
   [kouyou.db.core :as db]
   [markdown.common :as mdcom]
   [markdown.transformers :as mdtrans]]
  (:import java.lang.Integer)
  (:import java.lang.Character))


(defn escape-html
  "Change special characters into HTML character entities."
  [text {:keys [code codeblock] :as state}]
  [(if-not (or code codeblock)
     (str/escape
      text
      {\& "&amp;"
       \< "&lt;"
       \" "&quot;"
       \' "&#39;"})
     text) state])

;; There may be a cleaner way of doing this. I suggest a refactor.
;; I suggest pulling the nesting apart. Stop nesting calls and instead thread them.
;; Might as well do a regex match lol. Maybe this whole thing should just be regex ._.'
;; Also, no input checking???
;; This try-catch is too greedy. If the db call fails it silently misses it. Let's just get rid of the try-catches.
(defn post-reference [[level text] current_board]
  (let [[num & rest] (partition-by #(Character/isWhitespace %) text)
        joined_rest (apply str (map #(apply str %) rest)) ;; this isn't idiomatic
        joined_level (apply str level)
        escaped_level (str/escape joined_level {\> "&gt;"})]
    (try
      (let [reference (Integer/parseUnsignedInt (apply str num))]
        (if-let [id (:post_id (db/get-primary-post-id-from-nick-post {:post_id reference :nick current_board}))]
          (let [open (format "<a href=/boards/%s/res/%d#p%d>" current_board id reference)]
            (str open escaped_level (apply str num) "</a>" joined_rest))
          (str "<del>" escaped_level (apply str num) "</del>" joined_rest)))
      (catch Exception e (str escaped_level (apply str text))))))

;; We need the catch anyway as we can always overflow error. Actually, how do we want to deal with too many posts?
;; Swap to long for sure, but what about overflowing then?
;; What about doing a double check. First "is int" then "convert to int". It's slower but no catching
;; God this function is a dumpster fire
(defn cross-board-reference [[level text]]
  (let [[reference & rest] (partition-by #(Character/isWhitespace %) text)
        joined_rest (apply str (map #(apply str %) rest)) ;; this isn't idiomatic
        joined_level (apply str level)
        escaped_level (str/escape joined_level {\> "&gt;"})]
    (if-let [[_ board num] (re-matches #"/(.*)?/([0-9]+)" (apply str reference))]
      (try
        (if-let [id (:post_id (db/get-primary-post-id-from-nick-post {:post_id (Integer/parseUnsignedInt num) :nick board}))]
        (let [open (format "<a href=/boards/%s/res/%d#p%s>" board id num)]
          (str open escaped_level (apply str reference) "</a>" joined_rest))
        (str "<del>" escaped_level (apply str reference) "</del>" joined_rest))
        (catch Exception e (str escaped_level (apply str text))))
      (str escaped_level (apply str text)))))

(defn reference-sequencer [text]
  (->> (seq text)
       (partition-by #(= \> %))
       (partition-all 2)))

;; Next step is to pull out references that a post makes to advertise them in other posts
;; Then get cross-board references working
(defn format-reference [current_board [level text :as context]]
  (if (and (seq level) text) ;; This is a contextual check, this should be made more general.
    (case (count level)
      2 (post-reference context current_board) ;; can we share parts of these functions?
      3 (cross-board-reference context)
      (str (apply str level) (apply str text)))
    (apply str level)))

(defn reference-formatter [sequenced_text current_board]
  (->> sequenced_text
       (map (partial format-reference current_board))
       (apply str)))

;; We need to think about if the function should treat a failed reference ">>> test"
;; as a blockquote or not. 
(defn make-reference [current_board text]
  (let [sequenced_text (reference-sequencer text)
        formatted_text (reference-formatter sequenced_text current_board)]
    (if (= 1 (count (filter #(= \> %) (first (first sequenced_text)))))
      (str "<p><span class=implying>" formatted_text "</span></p>")
      formatted_text)))

(defn make-reference-transformer [current_board]
  (fn [text {:keys [code codeblock heading] :as state}]
    (if-not (or code codeblock heading)
      (if-let [reference-text (make-reference current_board text)]
        [reference-text state]
        [text state])
      [text state])))

(defn make-post-transformers [current_board]
  [escape-html
   mdtrans/set-line-state
   mdtrans/empty-line
   mdtrans/codeblock
   mdcom/escaped-chars
   mdcom/inline-code
   mdtrans/autourl-transformer
   (make-reference-transformer current_board)
   mdtrans/heading
   mdcom/bold
   mdcom/em
   mdcom/italics
   mdcom/strikethrough
   mdcom/strong
   mdtrans/paragraph
   mdcom/thaw-strings
   mdcom/dashes
   mdtrans/clear-line-state])
