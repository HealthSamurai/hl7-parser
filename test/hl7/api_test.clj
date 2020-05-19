(ns hl7.api-test
  (:require
   [hl7.core :as parser]
   [clojure.test :refer :all]))

(def test-message
  "MSH|^~\\&|MM^ModernizingMedicine|myclinic^|LKBRIDGE^Ellkay|ELLKAY^My Clinic|20200317131506||DFT^P03|20200317131506155|P|2.5.1\rEVN|P03|20200317131506\rPID||430153|430153||ALAN^KAY^^^||19950321|F|||||||||||\rPV1|1||6^^^My Clinic - Moscow^^^^^My Clinic - Moscow||||1639105331^Kent^Beck^^MD^||||||||||||30441341|||||||||||||||||||||||||||||||30441341\rFT1|1|||20200306|20200317131506|CG|11900^^CPT|||1||||||6^^^My Clinic - Moscow^^^^^My Clinic - Moscow|||L70.0^^I10|1111111111^Kent^Beck^^MD^|||||11900^^CPT||||\rFT1|2|||20200306|20200317131506|CG|J3301^^CPT|||1||||||6^^^My Clinic - Moscow^^^^^My Clinic - Moscow|||L70.0^^I10|1111111111^Kent^Beck^^MD^|||||J3301^^CPT||||")

(deftest api

  (parser/parse test-message)

  )

