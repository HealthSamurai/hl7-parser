(ns hl7.core
  (:require
   [hl7.utils :as utils]
   [hl7.segment :as segment]
   [clojure.string :as str])
  (:import [java.util.regex Pattern]))

(def rules (utils/load-meta "rules.edn"))

(defn mk-index [rules]
  (reduce (fn [acc [k {starts :starts :as group}]]
            (reduce (fn [acc [seg opts]]
                      (if (contains? acc seg)
                        (assert false (pr-str acc seg))
                        (assoc acc seg (assoc group :name k :opts opts))))
                    acc starts))
          {} rules))

(defn grouping [idx inp]
  (loop [parse-ctx nil
         cur-res {}
         [token & inp] inp
         res []]
    (if (nil? token)
      (if parse-ctx
        (conj res [parse-ctx cur-res])
        res)
      (if (and parse-ctx (contains? (:consumes parse-ctx) (first token)))
        (recur parse-ctx (merge cur-res (second token)) inp res)
        (let [res' (if parse-ctx (conj res [parse-ctx cur-res]) res)]
          (if-let [parse-ctx' (get idx (first token))]
            (recur parse-ctx' (second token) inp res')
            (do (println "ERROR" (str "No group for " token))
                (recur parse-ctx cur-res inp res))))))))

(defn structure [grps]
  (loop [gs grps
         next-iter []]
    (if (empty? gs)
      next-iter
      (let [[{nm1 :name} v1 :as n1] (first gs)
            [{inc-grp :groups :as g2} v2 :as n2] (second gs)]
        (if (nil? n2)
          (conj next-iter n1)
          (if-let [grp-opts (get inc-grp nm1)] 
            (let [next-iter (into (into next-iter [[g2 (update v2 nm1
                                                               (fn [old] (if old (if (sequential? old)
                                                                                   (conj old v1) [old v1])
                                                                             (if (:coll grp-opts)
                                                                               [v1] v1))))]])
                                  (drop 2 gs))]
              (recur next-iter []))
            (recur (rest gs) (conj next-iter n1))))))))

(defn *re-group [idx grps]
  (->>
   grps
   (mapv (fn [[{nm :name :as grp} val :as old]]
           (if-let [super-group (get idx nm)]
             [super-group {nm val}]
             old)))
   (structure)))

(defn re-group [idx grps]
  (loop [iter 0
         grps grps]
    (if (> iter 10)
      grps
      (let [new-grps  (*re-group idx grps)]
        (if (= new-grps grps)
          grps
          (recur (inc iter) new-grps))))))

(defn *parse [ctx inp]
  (let [idx  (mk-index rules)
        grps (grouping idx inp)
        final-grps (re-group idx (structure (reverse grps)))]
    (reduce (fn [acc [{nm :name} v]]
              (assoc acc nm v))
            {}
            final-grps)))

(defn parse-structure [msg]
  (*parse {} msg))

(defn separators [msg]
  {:segment #"(\r\n|\r|\n)"
   :field (get msg 3)
   :component (get msg 4)
   :subcomponet (get msg 7)
   :repetition (get msg 5)
   :escape (get msg 6)})

(defn pre-condition [msg]
  (cond-> []
    (not (str/starts-with? msg "MSH")) (conj "Message should start with MSH segment")
    (< (.length msg) 8) (conj "Message is too short (MSH truncated)")))

(defn split-by [s sep]
  (if (= java.util.regex.Pattern (type sep))
    (str/split s sep)
    (str/split s (re-pattern (Pattern/quote (str sep))))))

(defn parse-segments [msg]
  [msg]
  (let [msg (str/trim msg)
        errs (pre-condition msg)]
    (when-not (empty? errs)
      (throw (Exception. (str/join "; " errs))))
    (let [seps (separators msg)]
      (->> (split-by msg (:segment seps))
           (mapv str/trim)
           (filter #(> (.length %) 0))
           (mapv #(segment/parse-segment seps %))))))

(defn parse [msg]
  (parse-structure
   (parse-segments msg)))
