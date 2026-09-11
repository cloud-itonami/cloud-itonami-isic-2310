(ns glassworks.store-contract-test
  "The Store contract, run against BOTH backends. Proving MemStore and
  the Datomic-backed (langchain.db) store satisfy the same contract is
  what makes 'swap the SSoT for Datomic / kotoba-server' a
  configuration change, not a rewrite -- see `automotive.store-
  contract-test`/`bodyshop.store-contract-test` for the same pattern
  on sibling actors."
  (:require [clojure.test :refer [deftest is testing]]
            [glassworks.store :as store]))

(defn- backends []
  [["MemStore" (store/seed-db)] ["DatomicStore" (store/datomic-seed-db)]])

(deftest read-parity
  (doseq [[label s] (backends)]
    (testing label
      (is (= "Sakura Float-Line Windshield Lot WS-04" (:batch-name (store/batch s "batch-1"))))
      (is (= "JPN" (:jurisdiction (store/batch s "batch-1"))))
      (is (= :automotive-safety-glazing (:product-class (store/batch s "batch-1"))))
      (is (= 0.05 (:thickness-deviation-actual-mm (store/batch s "batch-1"))))
      (is (= -0.10 (:thickness-deviation-min-mm (store/batch s "batch-1"))))
      (is (= 0.10 (:thickness-deviation-max-mm (store/batch s "batch-1"))))
      (is (false? (:eol-defect-unresolved? (store/batch s "batch-1"))))
      (is (= 0.35 (:thickness-deviation-actual-mm (store/batch s "batch-3"))))
      (is (true? (:eol-defect-unresolved? (store/batch s "batch-4"))))
      (is (false? (:robotics-sim-verified? (store/batch s "batch-1"))) "no robotics mission has run yet")
      (is (true? (:robotics-sim-verified? (store/batch s "batch-5"))) "seeded as already-on-file")
      (is (= 45.0 (:sim-peak-flexural-stress-mpa (store/batch s "batch-5")))
          "REAL physics-2d-simulated telemetry, genuinely under-tempered")
      (is (= 160.125 (:sim-peak-flexural-stress-mpa (store/batch s "batch-1")))
          "REAL physics-2d-simulated telemetry, genuinely within [150,260] tempered spec")
      (is (= :cover-glass (:product-class (store/batch s "batch-6"))))
      (is (< 450.0 (:sim-peak-flexural-stress-mpa (store/batch s "batch-6")) 900.0)
          "cover-glass batch's real simulated flexural stress is genuinely within its own higher acceptance band")
      (is (false? (:glass-batch-shipped? (store/batch s "batch-1"))))
      (is (false? (:glazing-certified? (store/batch s "batch-1"))))
      (is (= ["batch-1" "batch-2" "batch-3" "batch-4" "batch-5" "batch-6"]
             (mapv :id (store/all-batches s))))
      (is (nil? (store/eol-screen-of s "batch-1")))
      (is (nil? (store/requirements-verification-of s "batch-1")))
      (is (= [] (store/ledger s)))
      (is (= [] (store/shipment-history s)))
      (is (= [] (store/evidence-history s)))
      (is (zero? (store/next-shipment-sequence s "JPN")))
      (is (zero? (store/next-evidence-sequence s "JPN")))
      (is (false? (store/batch-already-shipped? s "batch-1")))
      (is (false? (store/batch-already-certified? s "batch-1"))))))

(deftest write-and-ledger-parity
  (doseq [[label s] (backends)]
    (testing label
      (testing "partial upsert merges, preserving untouched fields"
        (store/commit-record! s {:effect :glass-panel-batch/upsert
                                 :value {:id "batch-1" :batch-name "Sakura Float-Line Windshield Lot WS-04"}})
        (is (= "Sakura Float-Line Windshield Lot WS-04" (:batch-name (store/batch s "batch-1"))))
        (is (= 0.05 (:thickness-deviation-actual-mm (store/batch s "batch-1"))) "unrelated field preserved"))
      (testing "robotics-sim result commits via :glass-panel-batch/upsert and reads back"
        (store/commit-record! s {:effect :glass-panel-batch/upsert
                                 :value {:id "batch-1" :robotics-sim-verified? true
                                        :sim-peak-flexural-force-n 854.0
                                        :sim-peak-flexural-stress-mpa 160.125
                                        :robotics-sim-record {:mission-id "m-1" :passed? true}}})
        (is (true? (:robotics-sim-verified? (store/batch s "batch-1"))))
        (is (= 160.125 (:sim-peak-flexural-stress-mpa (store/batch s "batch-1"))))
        (is (= {:mission-id "m-1" :passed? true} (:robotics-sim-record (store/batch s "batch-1"))))
        (is (= 0.05 (:thickness-deviation-actual-mm (store/batch s "batch-1"))) "unrelated field still preserved"))
      (testing "verification / EOL-screen payloads commit and read back"
        (store/commit-record! s {:effect :verification/set :path ["batch-1"]
                                 :payload {:jurisdiction "JPN" :checklist ["a" "b"]}})
        (is (= {:jurisdiction "JPN" :checklist ["a" "b"]} (store/requirements-verification-of s "batch-1")))
        (store/commit-record! s {:effect :eol-screen/set :path ["batch-1"]
                                 :payload {:batch-id "batch-1" :verdict :resolved}})
        (is (= {:batch-id "batch-1" :verdict :resolved} (store/eol-screen-of s "batch-1"))))
      (testing "glass-panel-batch shipment drafts a record and advances the sequence"
        (store/commit-record! s {:effect :glass-panel-batch/mark-shipped :path ["batch-1"]})
        (is (= "JPN-GLZ-000000" (get (first (store/shipment-history s)) "record_id")))
        (is (= "glass-panel-batch-shipment-draft" (get (first (store/shipment-history s)) "kind")))
        (is (true? (:glass-batch-shipped? (store/batch s "batch-1"))))
        (is (= 1 (count (store/shipment-history s))))
        (is (= 1 (store/next-shipment-sequence s "JPN")))
        (is (true? (store/batch-already-shipped? s "batch-1")))
        (is (false? (store/batch-already-shipped? s "batch-2"))))
      (testing "Glazing Certificate drafts a record and advances the sequence"
        (store/commit-record! s {:effect :glass-panel-batch/mark-certified :path ["batch-1"]})
        (is (= "JPN-GTC-000000" (get (first (store/evidence-history s)) "record_id")))
        (is (= "glazing-certificate-draft" (get (first (store/evidence-history s)) "kind")))
        (is (true? (:glazing-certified? (store/batch s "batch-1"))))
        (is (= 1 (count (store/evidence-history s))))
        (is (= 1 (store/next-evidence-sequence s "JPN")))
        (is (true? (store/batch-already-certified? s "batch-1")))
        (is (false? (store/batch-already-certified? s "batch-2"))))
      (testing "ledger is append-only and order-preserving"
        (store/append-ledger! s {:op :a :disposition :commit})
        (store/append-ledger! s {:op :b :disposition :hold})
        (is (= [:commit :hold] (mapv :disposition (store/ledger s))))))))

(deftest datomic-empty-store-is-usable
  (let [s (store/datomic-store)]
    (is (nil? (store/batch s "nope")))
    (is (= [] (store/all-batches s)))
    (is (= [] (store/ledger s)))
    (is (= [] (store/shipment-history s)))
    (is (= [] (store/evidence-history s)))
    (is (zero? (store/next-shipment-sequence s "JPN")))
    (is (zero? (store/next-evidence-sequence s "JPN")))
    (store/with-batches s {"x" {:id "x" :batch-name "n" :product-class :automotive-safety-glazing
                                :thickness-deviation-actual-mm 0.05
                                :thickness-deviation-min-mm -0.10 :thickness-deviation-max-mm 0.10
                                :eol-defect-unresolved? false
                                :glass-batch-shipped? false :glazing-certified? false
                                :jurisdiction "JPN" :status :intake}})
    (is (= "n" (:batch-name (store/batch s "x"))))))
