(ns hl7.segment-test
  (:require
   [hl7.segment :as sut]
   [matcho.core :as matcho]
   [clojure.test :refer :all]))

(def separators
  {:segment #"(\r\n|\r|\n)"
   :field "|"
   :component "^"
   :subcomponet "&"
   :repetition "~"
   :escape "\\"})

(deftest test-segment

  (matcho/match
   (sut/parse-segment
    separators
    "MSH|^~\\&|app|PTG|Billing System|Billing System|20200203125130352||ORM^O01|4131405852|P|2.3|||AL|AL ")
   [:MSH
    {:proc_id {:id "P"},
     :ack {:accept "AL"
           :app "AL"},
     :type {:code "ORM", :event "O01"},
     :id "4131405852",
     :sender {:app "app", :facility "PTG"},
     :datetime "2020-02-03T12:51:30.352",
     :version {:id "2.3"},
     :receiver {:app "Billing System", :facility "Billing System"}}])

  (matcho/match
   (sut/parse-segment
    separators
    "PID|1|010107111^^^MS4^PN^|1609220^^^MS4^MR^001|1609220^^^MS4^MR^001|BARRETT^JEAN^SANDY^^||19440823|F||C|STRAWBERRY AVE^FOUR OAKS LODGE^ALBUKERKA^CA^98765^USA^^||(111)222-3333||ENG|W|CHR|111155555550^^^MS4001^AN^001|123-22-1111||||OKLAHOMA|||||||N")

[:PID
 {:religion
  {:coding [{:use "primary", :code "CHR"}], :table "HL70006"},
  :address
  [{:line ["STRAWBERRY AVE"],
    :text "FOUR OAKS LODGE",
    :city "ALBUKERKA",
    :state "CA",
    :postalCode "98765",
    :country "USA"}],
  :_seqno "1",
  :race [{:coding [{:use "primary", :code "C"}]}],
  :name [{:family "BARRETT", :given ["JEAN" "SANDY"]}],
  :birthDate "1944-08-23",
  :language
  {:coding [{:use "primary", :code "ENG"}], :table "HL70296"},
  :marital_status
  {:coding [{:use "primary", :code "W"}], :table "HL70002"},
  :identifier
  [{:use "official",
    :value "010107111",
    :assigner {:authority "MS4"},
    :system "PN"}
   {:use "primary",
    :value "1609220",
    :assigner {:authority "MS4", :display "001"},
    :system "MR"}
   {:use "secondary",
    :value "1609220",
    :assigner {:authority "MS4", :display "001"},
    :system "MR"}
   {:system "account", :value "111155555550"}
   {:system "account", :assigner {:authority "MS4001", :display "001"}}
   {:system "AN"}
   {:system "ssn", :value "123-22-1111"}],
  :telecom [{:use "home", :system "phone", :value "(111)222-3333"}],
  :gender "F",
  :death {:indicator "N"},
  :birth {:place "OKLAHOMA"}}])

  (matcho/match
   (sut/parse-segment
    separators
    "PID|1||697909||Orange^Apple||19900214|F||^White|401 street^Apt 407^Xargo^CA^98980^USA^^^COUNTY||33333333^^^users@gmail.com|333333333|^English|||||||^No, Not Spanish/Hispanic/Latina||||||||N")
   [:PID
    {:address
     [{:line ["401 street"],
       :text "Apt 407",
       :city "Xargo",
       :state "CA",
       :postalCode "98980",
       :country "USA",
       :county "COUNTY"}],
     :_seqno "1",
     :race [{:coding [{:use "primary", :display "White"}]}],
     :name [{:family "Orange", :given ["Apple"]}],
     :birthDate "1990-02-14",
     :ethnicity
     [{:coding
       [{:use "primary", :display "No, Not Spanish/Hispanic/Latina"}]}],
     :language
     {:coding [{:use "primary", :display "English"}], :table "HL70296"},
     :identifier [{:use "primary", :value "697909"}],
     :telecom
     [{:use "home", :system "phone", :value "33333333"}
      {:use "work", :system "phone", :value [{:phone "333333333"}]}],
     :gender "F",
     :death {:indicator "N"}}])

  (matcho/match
   (sut/parse-segment
    separators
    "IN1|1|11111111^AMB-ANTHEM BLUE ACCESS-BRANSO-BBA^^^AMB-ANTHEM BLUE ACCESS-BRANSO-BBA|19010101|Missouri Blue Cross Blue Shield|PO BOX 33333^ANYWHERE^ATLANTA^GA^44444^CD:55555^B||1111111^WPN^CD:6666|77777||88888||20190101000000|||CD:999|john^doe^K^^^^CURRENT|CD:211111|19010101|2333333 STATE HIGHWAY T^^BTANTOT^MO^244444^CD:25555^M~2666 STATE HIGHWAY T^^BTANTOT^MO^27777^CD:28888^H~USER@COM.COM^^^^^^CD:2888|||1|||||||||||||CD:2999||||||||F|Anywhere^^Anywhere^MO^311111^CD:22133^B|||||A3222222")
[:IN1
 {:_seqno "1",
  :plan
  {:identifier
   {:coding
    [{:use "primary",
      :code "11111111",
      :display "AMB-ANTHEM BLUE ACCESS-BRANSO-BBA"}
     {:use "alt", :display "AMB-ANTHEM BLUE ACCESS-BRANSO-BBA"}],
    :table "HL70072"},
   :type "CD:999"},
  :payor
  {:identifier [{:value "19010101"}],
   :organization [{:name "Missouri Blue Cross Blue Shield"}],
   :address
   [{:line ["PO BOX 33333"],
     :text "ANYWHERE",
     :city "ATLANTA",
     :state "GA",
     :postalCode "44444",
     :country "CD:55555",
     :type "B"}],
   :contact
   {:telecom [{:phone "1111111", :use "WPN", :type "CD:6666"}]}},
  :group {:id "77777", :employee {:identifier [{:value "88888"}]}},
  :period {:start "2019-01-01T00:00:00"},
  :beneficiary
  {:name [{:family "john", :given ["doe" "K"], :use "CURRENT"}],
   :relationship
   {:coding [{:use "primary", :code "CD:211111"}], :table "HL70063"},
   :birth_date "1901-01-01",
   :address
   [{:line ["2333333 STATE HIGHWAY T"],
     :city "BTANTOT",
     :state "MO",
     :postalCode "244444",
     :country "CD:25555",
     :type "M"}
    {:line ["2666 STATE HIGHWAY T"],
     :city "BTANTOT",
     :state "MO",
     :postalCode "27777",
     :country "CD:28888",
     :type "H"}
    {:line ["USER@COM.COM"], :type "CD:2888"}],
   :gender "F",
   :employer
   {:address
    [{:line ["Anywhere"],
      :city "Anywhere",
      :state "MO",
      :postalCode "311111",
      :country "CD:22133",
      :type "B"}]},
   :subscriber_id [{:value "A3222222"}]},
  :benifits {:priority "1"},
  :company_plan "CD:2999"}])

  (matcho/match
   (sut/parse-segment
    separators
    "IN2|empid:11111|ssn:222|||||||||||||||||||||||payor:3333||||||||||||||||||||||||||||||||||||mem:3333||(555)5555-4384^PRN^CD:193972~(555)555-4384^CP^CD:78193972~(555)555-4384^CP^CD:78193972|(111)111-1111^WPN||||||||CD:24246")
   [:IN2
    {:beneficiary
     {:employee {:identifier [{:value "empid:11111"}]},
      :identifier [{:system "ssn", :value "ssn:222"} {:value "mem:3333"}],
      :telecom
      [[{:phone "(555)5555-4384", :use "PRN", :type "CD:193972"}
        {:phone "(555)555-4384", :use "CP", :type "CD:78193972"}
        {:phone "(555)555-4384", :use "CP", :type "CD:78193972"}]
       [{:phone "(111)111-1111", :use "WPN"}]]},
     :payor {:id [{:value "payor:3333"}]},
     :cms
     {:relationship
      {:coding [{:use "primary", :code "CD:24246"}], :table "HL70344"}}}])

  (matcho/match
   (sut/parse-segment
    separators
    "IN2|empid:11111|ssn:222|||||||||||||||||||||||payor:3333||||||||||||||||||||||||||||||||||||mem:3333||(555)5555-4384^PRN^CD:193972~(555)555-4384^CP^CD:78193972~(555)555-4384^CP^CD:78193972|(111)111-1111^WPN||||||||CD:24246")
   [:IN2
    {:beneficiary
     {:employee {:identifier [{:value "empid:11111"}]},
      :identifier [{:system "ssn", :value "ssn:222"} {:value "mem:3333"}],
      :telecom
      [[{:phone "(555)5555-4384", :use "PRN", :type "CD:193972"}
        {:phone "(555)555-4384", :use "CP", :type "CD:78193972"}
        {:phone "(555)555-4384", :use "CP", :type "CD:78193972"}]
       [{:phone "(111)111-1111", :use "WPN"}]]},
     :payor {:id [{:value "payor:3333"}]},
     :cms
     {:relationship
      {:coding [{:use "primary", :code "CD:24246"}], :table "HL70344"}}}])

  (matcho/match
   (sut/parse-segment
    separators
    "OBX|2|DT|41651-1^date^LN||20110529130917-04:00|mg/dL|||||F|||20150529130917-04:00||APOC3214||||MIX")
   [:OBX
    {:_seqno "2",
     :value
     {:type "DT",
      :value "2011-05-29T13:09:17.-04:00",
      :unit {:coding [{:use "primary", :code "mg/dL"}]}},
     :code
     {:coding
      [{:use "primary", :code "41651-1", :display "date", :system "LN"}]},
     :status "F",
     :performer {:responsible [{:identifier {:value "APOC3214"}}]},
     :body_site [{:code "MIX"}]}])

  (matcho/match
   (sut/parse-segment
    separators
    "OBX|3|ST|45541-1^GJU^LN||100500 mm||||||F|||||APOC3214|||20110529130917-04:00|MIX")
   [:OBX
    {:_seqno "3",
     :value {:type "ST", :value "100500 mm"},
     :code
     {:coding
      [{:use "primary", :code "45541-1", :display "GJU", :system "LN"}]},
     :status "F",
     :performer {:responsible [{:identifier {:value "APOC3214"}}]},
     :issued "2011-05-29T13:09:17.-04:00",
     :body_site [{:code "MIX"}]}])

  (matcho/match
   (sut/parse-segment
    separators
    "OBX|1|NM|8625-6^P-R interval^LN||<10|%PCV||N|||F|||||APOC3214||equipment|20120529130917-04:00|MIX ")
   [:OBX
    {:_seqno "1",
     :interpretation {:flag ["N"]},
     :value
     {:type "NM",
      :value "<10",
      :unit {:coding [{:use "primary", :code "%PCV"}]}},
     :body_site [{:code "MIX"}],
     :status "F",
     :code
     {:coding
      [{:use "primary",
        :code "8625-6",
        :display "P-R interval",
        :system "LN"}]},
     :issued "2012-05-29T13:09:17.-04:00",
     :device [{:identifier "equipment"}],
     :performer {:responsible [{:identifier {:value "APOC3214"}}]}}])

  (matcho/match
   (sut/parse-segment
    separators
    "OBX|7|ST|PAPHX^Previous History||Hx of Normal Paps")
   [:OBX
    {:_seqno "7",
     :value {:type "ST", :value "Hx of Normal Paps"},
     :code
     {:coding
      [{:use "primary", :code "PAPHX", :display "Previous History"}]}}]))


