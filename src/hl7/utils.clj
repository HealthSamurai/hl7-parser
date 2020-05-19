(ns hl7.utils
  (:require
   [clojure.java.io :as io]))

(defn load-meta [nm]
  (if-let [f (io/resource nm)] 
    (read-string (slurp f))
    (throw (Exception. (str nm " not found")))))
