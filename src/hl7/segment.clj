(ns hl7.segment
  (:require
   [hl7.utils :as utils]
   [mappers.simple]
   [clojure.string :as str])
  (:import [java.util.regex Pattern]))

(def metadata (utils/load-meta "segments.edn"))
(def types (utils/load-meta "types.edn"))

(defn separators [msg]
  {:segment #"(\r\n|\r|\n)"
   :field (get msg 3)
   :component (get msg 4)
   :subcomponet (get msg 7)
   :repetition (get msg 5)
   :escape (get msg 6)})

(defn split-by [s sep]
  (if (= java.util.regex.Pattern (type sep))
    (str/split s sep)
    (str/split s (re-pattern (Pattern/quote (str sep))))))

(defn indexed-map [coll]
  (loop [[x & xs] coll acc {} idx 1]
    (if (empty? xs)
      (if (nil? x) acc (assoc acc idx x))
      (recur xs
             (if (nil? x) acc (assoc acc idx x))
             (inc idx)))))

(defn not-empty? [v]
  (if (or (map? v) (sequential? v))
    (not (empty? v))
    (if (string? v)
      (not (str/blank? v))
      (not (nil? v)))))

(def primitives
  {:DT :timestamp
   :DTM true
   :FT true
   :GTS true
   :ID true
   :IS true
   :NM true
   :NUL true
   :SI true
   :HD true ;; hack
   :ST true
   :TM true
   :TX true
   :TS :timestamp
   :escapeType true})

(defn put-in [acc k v]
  (mappers.simple/put-in acc (if (sequential? k) k [k]) v))

(defmulti parse-primitive (fn [k _] k))


(defmethod parse-primitive
  :timestamp
  [_ v]
  (let [slen (count v)]
    (str 
     (subs v 0 4)
     "-"
     (subs v 4 6)
     (if (> slen 6)
       (str "-" (subs v 6 8)
            (when (> slen 8)
              (str "T"
                   (subs v 8 10)
                   ":"
                   (subs v 10 12)
                   (if (> slen 12)
                     (str ":" (subs v 12 14)
                          (when (> slen 14)
                            (str "." (subs v 14))))
                     ":00"))))
       "-01"))))

(parse-primitive :timestamp "201912271415")

(defn parse-value [sep tp v]
  (if-let [prim (get primitives (keyword (:type tp)))]
    (if (keyword? prim)
      (parse-primitive prim (str/trim v))
      (str/trim v))
    (if-let [sub-tp (get types (keyword (:type tp)))]
      (if (:collection sub-tp)
        (filterv #(not (str/blank? (str/trim %)))
                 (split-by v (:subcomponet sep)))
        (let [sub-cmps (split-by v (:subcomponet sep))
              sub-types (:components sub-tp)
              parsed-value (loop [[c & cs] sub-cmps
                                  [s & ss] sub-types
                                  res {}]
                             (let [res (if-not (str/blank? c)
                                         (let [v (parse-value sep s c)]
                                           (if (not-empty? v)
                                             (assoc res (keyword (:key s)) (str/trim v))
                                             res))
                                         res)]
                               (if (empty? cs)
                                 res
                                 (recur cs ss res))))]
          (if-let [only-key (:only sub-tp)]
            (do
              (when (< 1 (count parsed-value))
                (println "WARN: " (str "Expected only one value " only-key " got " (pr-str parsed-value))))
              (get parsed-value (keyword only-key)))
            
            parsed-value)))
      (str/trim v))))


(defn parse-component [seps tp v]
  (if (:components tp)
    (let [cmps (split-by v (:component seps))]
      (loop [[c & cs] cmps
             [s & ss] (:components tp)
             res {}]
        (let [res (if-not (str/blank? c)
                    (let [v (parse-value seps s c)]
                      (if (not-empty? v)
                        (put-in res (:key s) v)
                        res))
                    res)]
          (if (empty? cs)
            res
            (recur cs ss res)))))
    (parse-value seps tp (str/trim v))))

(defn parse-field [seps {tpn :type c? :coll v :value tp-ref :type_ref :as f}]
  (let [tp (get types (keyword tpn))
        vv (if c?
             (->> (split-by v (:repetition seps))
                  (mapv #(parse-component seps tp %))
                  (filterv not-empty?))
             (parse-component seps (or tp {:type tpn}) v))]
    vv))

(defn parse-segment [seps seg]
  (let [fields (split-by seg (:field seps))
        [seg-name & fields] fields]
    (if (str/starts-with? seg-name "Z")
      [(keyword seg-name) {:value fields}]

      (let [fields (if (= "MSH" seg-name) (into ["|"] fields) fields)
            seg-sch (get metadata (keyword seg-name))]
        (when-not seg-sch
          (throw (Exception. (str "No schema for " seg-name)) ))
        [(keyword seg-name)
         (loop [[s & ss] seg-sch
                field-idx 0
                acc {}]
           (when (nil? s)
             (throw (Exception. (pr-str seg-name "should have a schema"))))
           (let [f (nth fields field-idx nil)]
             (if (str/blank? f)
               (if (empty? ss)
                 acc
                 (recur ss (inc field-idx) acc))
               (let [field-schema (if (= (:type s) "varies")
                                    (if-let [tp (get-in acc (mapv keyword (:type_ref s)))]
                                      (assoc s :type tp)
                                      (do 
                                        (println "WARN:"  "No type_ref" s " value " acc)
                                        (assoc s :type "ST")))
                                    s)
                     v (parse-field seps (assoc field-schema :value f))
                     v (if-let [tbl  (and (map? v) (:table field-schema))]
                         (assoc v :table tbl)
                         v)
                     acc  (if (not-empty? v)
                            (let [k (:key s)]
                              (put-in acc k v))
                            acc)]
                 (if (empty? ss)
                   acc
                   (recur ss (inc field-idx) acc))))))]))))
