(ns glassworks.registry-test
  (:require [clojure.test :refer [deftest is]]
            [glassworks.registry :as r]))

;; ----------------------------- panel-thickness-deviation-out-of-range? -----------------------------

(deftest not-out-of-range-when-within-bounds
  (is (not (r/panel-thickness-deviation-out-of-range? {:thickness-deviation-actual-mm 0.05 :thickness-deviation-min-mm -0.10 :thickness-deviation-max-mm 0.10})))
  (is (not (r/panel-thickness-deviation-out-of-range? {:thickness-deviation-actual-mm -0.10 :thickness-deviation-min-mm -0.10 :thickness-deviation-max-mm 0.10})))
  (is (not (r/panel-thickness-deviation-out-of-range? {:thickness-deviation-actual-mm 0.10 :thickness-deviation-min-mm -0.10 :thickness-deviation-max-mm 0.10}))))

(deftest out-of-range-when-below-minimum-or-above-maximum
  (is (r/panel-thickness-deviation-out-of-range? {:thickness-deviation-actual-mm -0.35 :thickness-deviation-min-mm -0.10 :thickness-deviation-max-mm 0.10}))
  (is (r/panel-thickness-deviation-out-of-range? {:thickness-deviation-actual-mm 0.35 :thickness-deviation-min-mm -0.10 :thickness-deviation-max-mm 0.10})))

(deftest out-of-range-is-false-on-missing-fields
  (is (not (r/panel-thickness-deviation-out-of-range? {})))
  (is (not (r/panel-thickness-deviation-out-of-range? {:thickness-deviation-actual-mm 0.35}))))

;; ----------------------------- register-glass-panel-batch-shipment -----------------------------

(deftest shipment-is-a-draft-not-a-real-dispatch
  (let [result (r/register-glass-panel-batch-shipment "batch-1" "JPN" 0 :automotive-safety-glazing)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest shipment-assigns-shipment-number
  (let [result (r/register-glass-panel-batch-shipment "batch-1" "JPN" 7 :automotive-safety-glazing)]
    (is (= (get result "shipment_number") "JPN-GLZ-000007"))
    (is (= (get-in result ["record" "batch_id"]) "batch-1"))
    (is (= (get-in result ["record" "kind"]) "glass-panel-batch-shipment-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest shipment-records-real-downstream-consumers-by-product-class
  (is (= ["cloud-itonami-isic-2910" "cloud-itonami-isic-2920"]
         (get-in (r/register-glass-panel-batch-shipment "batch-1" "JPN" 0 :automotive-safety-glazing)
                 ["record" "downstream_consumers"])))
  (is (= ["cloud-itonami-isic-2630"]
         (get-in (r/register-glass-panel-batch-shipment "batch-6" "COVER-GLASS" 0 :cover-glass)
                 ["record" "downstream_consumers"]))))

(deftest shipment-validation-rules
  (is (thrown? Exception (r/register-glass-panel-batch-shipment "" "JPN" 0 :automotive-safety-glazing)))
  (is (thrown? Exception (r/register-glass-panel-batch-shipment "batch-1" "" 0 :automotive-safety-glazing)))
  (is (thrown? Exception (r/register-glass-panel-batch-shipment "batch-1" "JPN" -1 :automotive-safety-glazing))))

;; ----------------------------- register-glazing-certificate -----------------------------

(deftest certificate-is-a-draft-not-real-certification
  (let [result (r/register-glazing-certificate "batch-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest certificate-assigns-evidence-number
  (let [result (r/register-glazing-certificate "batch-1" "JPN" 3)]
    (is (= (get result "evidence_number") "JPN-GTC-000003"))
    (is (= (get-in result ["record" "batch_id"]) "batch-1"))
    (is (= (get-in result ["record" "kind"]) "glazing-certificate-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest certificate-validation-rules
  (is (thrown? Exception (r/register-glazing-certificate "" "JPN" 0)))
  (is (thrown? Exception (r/register-glazing-certificate "batch-1" "" 0)))
  (is (thrown? Exception (r/register-glazing-certificate "batch-1" "JPN" -1))))

(deftest history-is-append-only
  (let [c1 (r/register-glass-panel-batch-shipment "batch-1" "JPN" 0 :automotive-safety-glazing)
        hist (r/append [] c1)
        c2 (r/register-glass-panel-batch-shipment "batch-2" "JPN" 1 :automotive-safety-glazing)
        hist2 (r/append hist c2)]
    (is (= 2 (count hist2)))
    (is (= "JPN-GLZ-000000" (get-in hist2 [0 "record_id"])))
    (is (= "JPN-GLZ-000001" (get-in hist2 [1 "record_id"])))))
