(ns glassworks.sim
  "Demo driver -- `clojure -M:dev:run`. Walks a clean automotive-safety-
  glazing batch through intake -> glazing-standard requirements
  verification -> end-of-line-defect screening -> robot flexural-bend-
  test simulation -> batch-shipment proposal (always escalates) ->
  human approval -> commit, then through Glazing-Certificate proposal
  (always escalates) -> human approval -> commit; walks a clean
  cover-glass batch through the SAME full lifecycle to a downstream
  `cloud-itonami-isic-2630` shipment; then shows six HARD holds (a
  jurisdiction with no spec-basis, an actuation attempt before the
  robot mission ever ran, an out-of-spec thickness deviation, a robot
  flexural-bend-test simulation that is genuinely UNDER-TEMPERED on
  independent recheck, an unresolved end-of-line defect screened
  directly via `:end-of-line-quality/screen` [never via an actuation
  op against an unscreened batch -- see this actor's own governor ns
  docstring / the lesson every prior sibling's ADR-0001 already
  recorded], and a double batch-shipment/certificate-issuance of an
  already-processed batch) that never reach a human at all, and prints
  the audit ledger + the draft glass-panel-batch-shipment and Glazing-
  Certificate records."
  (:require [langgraph.graph :as g]
            [glassworks.export :as export]
            [glassworks.store :as store]
            [glassworks.operation :as op]))

(def operator {:actor-id "op-1" :actor-role :glazing-quality-engineer :phase 3})

(defn- exec! [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn -main [& _]
  (let [db (store/seed-db)
        actor (op/build db)]
    (println "== glass-panel-batch/intake batch-1 (JPN, automotive-tempered; clean flexural + thickness) ==")
    (println (exec! actor "t1" {:op :glass-panel-batch/intake :subject "batch-1"
                                :patch {:id "batch-1" :batch-name "Sakura Float-Line Windshield Lot WS-04"}} operator))

    (println "== glazing-standard-rules/verify batch-1 (escalates -- human approves) ==")
    (println (exec! actor "t2" {:op :glazing-standard-rules/verify :subject "batch-1"} operator))
    (println (approve! actor "t2"))

    (println "== end-of-line-quality/screen batch-1 (clean; escalates -- human approves) ==")
    (println (exec! actor "t3" {:op :end-of-line-quality/screen :subject "batch-1"} operator))
    (println (approve! actor "t3"))

    (println "== robotics/simulate-flexural-strength-test batch-1 (REAL physics-2d bend-test mission; escalates -- human approves) ==")
    (println (exec! actor "t3b" {:op :robotics/simulate-flexural-strength-test :subject "batch-1"} operator))
    (println (approve! actor "t3b"))

    (println "== actuation/ship-glass-panel-batch batch-1 (always escalates) ==")
    (let [r (exec! actor "t4" {:op :actuation/ship-glass-panel-batch :subject "batch-1"} operator)]
      (println r)
      (println "-- human glazing quality engineer approves --")
      (println (approve! actor "t4")))

    (println "== actuation/issue-glazing-certificate batch-1 (always escalates) ==")
    (let [r (exec! actor "t5" {:op :actuation/issue-glazing-certificate :subject "batch-1"} operator)]
      (println r)
      (println "-- human glazing quality engineer approves --")
      (println (approve! actor "t5")))

    (println "== cover-glass batch-6 full lifecycle (downstream -> cloud-itonami-isic-2630) ==")
    (println (exec! actor "t6a" {:op :glazing-standard-rules/verify :subject "batch-6"} operator))
    (println (approve! actor "t6a"))
    (println (exec! actor "t6b" {:op :end-of-line-quality/screen :subject "batch-6"} operator))
    (println (approve! actor "t6b"))
    (println (exec! actor "t6c" {:op :robotics/simulate-flexural-strength-test :subject "batch-6"} operator))
    (println (approve! actor "t6c"))
    (let [r (exec! actor "t6d" {:op :actuation/ship-glass-panel-batch :subject "batch-6"} operator)]
      (println r)
      (println (approve! actor "t6d")))
    (let [r (exec! actor "t6e" {:op :actuation/issue-glazing-certificate :subject "batch-6"} operator)]
      (println r)
      (println (approve! actor "t6e")))

    (println "== glazing-standard-rules/verify batch-2 (no spec-basis -> HARD hold) ==")
    (println (exec! actor "t7" {:op :glazing-standard-rules/verify :subject "batch-2" :no-spec? true} operator))

    (println "== glazing-standard-rules/verify batch-3 (escalates -- human approves; sets up the out-of-spec thickness test) ==")
    (println (exec! actor "t8" {:op :glazing-standard-rules/verify :subject "batch-3"} operator))
    (println (approve! actor "t8"))

    (println "== actuation/ship-glass-panel-batch batch-3 before robotics simulation -> HARD hold (robotics-simulation-missing) ==")
    (println (exec! actor "t8b" {:op :actuation/ship-glass-panel-batch :subject "batch-3"} operator))

    (println "== robotics/simulate-flexural-strength-test batch-3 (clean flexural stress; escalates -- human approves) ==")
    (println (exec! actor "t8c" {:op :robotics/simulate-flexural-strength-test :subject "batch-3"} operator))
    (println (approve! actor "t8c"))

    (println "== actuation/ship-glass-panel-batch batch-3 (0.35mm outside [-0.10,0.10]mm thickness-deviation tolerance -> HARD hold) ==")
    (println (exec! actor "t9" {:op :actuation/ship-glass-panel-batch :subject "batch-3"} operator))

    (println "== actuation/ship-glass-panel-batch batch-5 (robotics-sim on file, but REAL simulated flexural stress 45.0MPa genuinely UNDER-TEMPERED vs. [150,260]MPa spec on independent recheck -> HARD hold) ==")
    (println (exec! actor "t9b" {:op :glazing-standard-rules/verify :subject "batch-5"} operator))
    (println (approve! actor "t9b"))
    (println (exec! actor "t9c" {:op :actuation/ship-glass-panel-batch :subject "batch-5"} operator))

    (println "== end-of-line-quality/screen batch-4 (unresolved -> HARD hold, never reaches a human) ==")
    (println (exec! actor "t10" {:op :end-of-line-quality/screen :subject "batch-4"} operator))

    (println "== actuation/ship-glass-panel-batch batch-1 AGAIN (double-shipment -> HARD hold) ==")
    (println (exec! actor "t11" {:op :actuation/ship-glass-panel-batch :subject "batch-1"} operator))

    (println "== actuation/issue-glazing-certificate batch-1 AGAIN (double-issuance -> HARD hold) ==")
    (println (exec! actor "t12" {:op :actuation/issue-glazing-certificate :subject "batch-1"} operator))

    (println "== audit ledger ==")
    (doseq [f (store/ledger db)] (println f))

    (println "== draft glass-panel-batch-shipment records ==")
    (doseq [r (store/shipment-history db)] (println r))

    (println "== draft Glazing Certificate records ==")
    (doseq [r (store/evidence-history db)] (println r))

    (println "== social hand-off: audit package counts ==")
    (println (:counts (export/audit-package db)))
    (println "== social hand-off: CSV bundle keys ==")
    (println (keys (export/package->csv-bundle db)))))
