(ns hl7.core-test
  (:require
   [matcho.core :as matcho]
   [hl7.core :as sut]
   [clojure.test :refer :all]))

(deftest hl7-parse-structurer-test

  (matcho/match
   (sut/parse-structure [[:PID {:name 1}]])
   {:patient_group {:patient {:name 1}}})

  (matcho/match
   (sut/parse-structure [[:PID {:name 1}] [:IN1 {:in1 1}]])
   {:patient_group
    {:patient {:name 1}
     :insurances [{:in1 1}]}})

  (matcho/match
   (sut/parse-structure [[:PID {:name 1}] [:IN1 {:in1 1}] [:IN2 {:in2 1}]])
   {:patient_group 
    {:patient {:name 1}
     :insurances [{:in1 1 :in2 1}]}})

  (matcho/match
   (sut/parse-structure [[:PID {:name 1}] [:IN1 {:in1 1}]  [:IN1 {:in1 2}] [:IN2 {:in2 2}]])
   {:patient_group
    {:patient {:name 1}
     :insurances [{:in1 1}
                  {:in1 2 :in2 2}]}})

  (matcho/match
   (sut/parse-structure [[:PID {:name 1}] [:IN1 {:in1 1}] [:GT1 {:gt1 1}]])
   {:patient_group
    {:patient {:name 1}
     :insurances [{:in1 1}]
     :guarantors [{:gt1 1}]}})

  (matcho/match
   (sut/parse-structure [[:PID {:name 1}] [:IN1 {:in1 1}] [:GT1 {:gt1 1}] [:IN1 {:in1 2}] [:GT1 {:gt1 2}]])
   {:patient_group
    {:patient {:name 1}
     :insurances [{:in1 1} {:in1 2}]
     :guarantors [{:gt1 1}
                  {:gt1 2}]}})

  (matcho/match
   (sut/parse-structure [[:PID {:name 1}] [:PV1 {:pv1 1}]])
   {:encounter_procedures
    {:visit {:pv1 1}}
    :patient_group
    {:patient {:name 1}}})

  (matcho/match
   (sut/parse-structure [[:PID {:name 1}] [:IN1 {:in1 1}] [:IN1 {:in1 2}] [:GT1 {:gt1 1}] [:GT1 {:gt1 2}]])
   {:patient_group
    {:patient {:name 1}
     :insurances [{:in1 1} {:in1 2}]
     :guarantors [{:gt1 1} {:gt1 2}]}})

  (matcho/match
   (sut/parse-structure [[:PID {:name 1}] [:IN1 {:in1 1}] [:IN1 {:in1 2}] [:GT1 {:gt1 1}] [:GT1 {:gt1 2}] [:ORC {:orc 1}]])
   {:patient_group
    {:patient {:name 1}
     :insurances [{:in1 1} {:in1 2}]
     :guarantors [{:gt1 1} {:gt1 2}]
     :order_group [{:order {:orc 1}}]}})

  (matcho/match
   (sut/parse-structure [[:PID {:name 1}] [:ORC {:orc 1}] [:OBR {:obr 1}]])
   {:patient_group {:patient {:name 1}
                    :order_group [{:order {:orc 1}
                                   :observation_request [{:obr 1}]}]}})

  (matcho/match
   (sut/parse-structure [[:PID {:name 1}] [:ORC {:orc 1}] [:OBR {:obr 1}] [:OBX {:obx 1}] [:OBX {:obx 2}]])
   {:patient_group
    {:patient {:name 1}
     :order_group [{:order {:orc 1}
                    :observation_request [{:obr 1 :observations [{:obx 1} {:obx 2}]}]}]}})


  (matcho/match
   (sut/parse-structure [[:PID {:name 1}]
                         [:ORC {:orc 1}]
                         [:OBR {:obr 1}]
                         [:OBX {:obx 1}] [:OBX {:obx 2}]
                         [:ORC {:orc 2}]
                         [:OBX {:obx 21}]
                         [:OBX {:obx 22}]])
   {:patient_group
    {:patient {:name 1}
     :order_group [{:order {:orc 1}
                    :observation_request [{:observations [{:obx 1} {:obx 2}]}]}
                   {:order {:orc 2
                            :observations [{:obx 21} {:obx 22}]}}]}})

  (matcho/match
   (sut/parse-structure [[:PID {:name 1}]
               [:OBX {:obx 0}]
               [:ORC {:orc 1}]
               [:OBR {:obr 1}]
               [:OBX {:obx 1}] [:OBX {:obx 2}]])
   {:patient_group
    {:patient {:name 1}
     :observations [{:obx 0}]
     :order_group [{:order {:orc 1}
                    :observation_request [{:obr 1
                                           :observations [{:obx 1} {:obx 2}]}]}]}}))


