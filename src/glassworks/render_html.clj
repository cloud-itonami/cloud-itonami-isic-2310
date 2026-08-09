(ns glassworks.render-html
  "Build-time HTML renderer for `docs/samples/operator-console.html`.

  Closes flagship checklist item 2 (com-junkawasaki/root ADR-2607189300,
  Wave5 rollout template: `90-docs/business/cloud-itonami-flagship-
  generator-template.edn`). This repo previously had either a HAND-AUTHORED
  `docs/samples/operator-console.html` or a thin incomplete generator that
  shipped batch-1 without running the robot flexural mission (so the only
  actuation path was a HARD hold on `:robotics-simulation-missing`). This
  namespace REPLACES that with a genuine build-time generator that drives
  the REAL actor stack (`glassworks.operation` -> `glassworks.governor` ->
  `glassworks.store`) through a scenario adapted from this repo's own
  `glassworks.sim` demo driver (`clojure -M:dev:run`, confirmed by reading
  the driver directly -- its ids (batch-1..6), ops and violation rule
  names all match `glassworks.store/demo-data`'s real seed data and
  `glassworks.governor`'s real check functions), covering:

    - one full phase-3-auto intake + escalate/approve lifecycle building
      batch-1 up to a real glass-panel-batch shipment + Glazing Certificate
    - cover-glass batch-6 through the same full lifecycle (downstream
      `cloud-itonami-isic-2630`)
    - six distinct HARD-hold reasons that never reach a human
      (`:no-spec-basis`, `:robotics-simulation-missing`,
      `:panel-thickness-deviation-out-of-range`,
      `:robotics-simulation-out-of-tolerance`,
      `:end-of-line-defect-unresolved`, `:already-shipped` /
      `:already-certified`)

  Rendered deterministically -- no invented numbers, no timestamps in the
  page content, byte-identical across reruns against the same seed. The
  `:robotics-simulation-out-of-tolerance` hold below is driven by a REAL
  `physics-2d`-stepped simulation result (batch-5's own recorded
  `:flexural-test-pin-mass-kg`/`:panel-thickness-actual-mm` genuinely
  produce a 45.0 MPa peak flexural stress against the [150,260] MPa
  automotive-tempered band, `glassworks.robotics`/ADR-2607151600/ADR-
  2607152000) -- not a hand-typed number.

  Usage: `clojure -M:dev:render-html [out-file]`
  (default `docs/samples/operator-console.html`)."
  (:require [jp-go-dds.skin]
            [clojure.string :as str]
            [glassworks.store :as store]
            [glassworks.operation :as op]
            [langgraph.graph :as g]))

;; ----------------------------- harness (unchanged across every repo
;; in this cluster -- do not rewrite, only copy) -----------------------

(def ^:private operator
  {:actor-id "op-1" :actor-role :glazing-quality-engineer :phase 3})

(defn- exec! [actor tid request]
  (g/run* actor {:request request :context operator} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}}
          {:thread-id tid :resume? true}))

(defn run-demo!
  "Runs a fresh seeded store through a scenario mixing every disposition
  this actor can reach. Mirrors `glassworks.sim` (ids/ops/rule names
  taken from that driver and the seed data it exercises).

  Clean path (batch-1 automotive-tempered JPN):
    - `:glass-panel-batch/intake` normalizes the batch record
      (governor-clean, high confidence, phase-3 `:auto` -- the ONLY op
      in `glassworks.phase`'s phase-3 auto set -- auto-commits, no human).
    - `:glazing-standard-rules/verify` drafts the JIS R 3211 evidence
      checklist (governor-clean, not auto-eligible -- escalates, approved).
    - `:end-of-line-quality/screen` finds no unresolved defect
      (governor-clean -- escalates, approved).
    - `:robotics/simulate-flexural-strength-test` runs the REAL ASTM C158
      four-point flexural bend-test `physics-2d` mission (passes --
      escalates, approved).
    - `:actuation/ship-glass-panel-batch` is ALWAYS high-stakes per
      `glassworks.governor/high-stakes` -- escalates, approved, commits a
      real glass-panel-batch-shipment draft.
    - `:actuation/issue-glazing-certificate` is likewise ALWAYS
      high-stakes -- escalates, approved, commits a real Glazing
      Certificate draft.

  Clean path (batch-6 cover-glass COVER-GLASS, downstream
  cloud-itonami-isic-2630): same lifecycle without re-intake.

  HARD-hold paths (never reach a human):
    - batch-2 `:glazing-standard-rules/verify` with `:no-spec?` against
      jurisdiction \"ATL\" (no catalog entry) -> `:no-spec-basis`.
    - batch-3 `:actuation/ship-glass-panel-batch` before any robot
      mission -> `:robotics-simulation-missing`.
    - batch-3 after clean verify + robotics mission -- thickness
      deviation 0.35 mm outside [-0.10,0.10] mm ->
      `:panel-thickness-deviation-out-of-range`.
    - batch-5 after verify -- REAL physics-2d recheck yields 45.0 MPa
      under the 150.0 MPa floor even though `:robotics-sim-verified?`
      was seeded true -> `:robotics-simulation-out-of-tolerance`.
    - batch-4 `:end-of-line-quality/screen` with
      `:eol-defect-unresolved? true` -> `:end-of-line-defect-unresolved`.
    - batch-1 ship/cert AGAIN after already processed ->
      `:already-shipped` / `:already-certified`.

  Returns the resulting store -- every field `render` below reads is
  real governor/store output, not a hand-typed copy."
  []
  (let [db (store/seed-db)
        actor (op/build db)]

    ;; ---- batch-1 full clean lifecycle ----
    (exec! actor "t1" {:op :glass-panel-batch/intake :subject "batch-1"
                       :patch {:id "batch-1" :batch-name "Sakura Float-Line Windshield Lot WS-04"}})

    (exec! actor "t2" {:op :glazing-standard-rules/verify :subject "batch-1"})
    (approve! actor "t2")

    (exec! actor "t3" {:op :end-of-line-quality/screen :subject "batch-1"})
    (approve! actor "t3")

    (exec! actor "t3b" {:op :robotics/simulate-flexural-strength-test :subject "batch-1"})
    (approve! actor "t3b")

    (exec! actor "t4" {:op :actuation/ship-glass-panel-batch :subject "batch-1"})
    (approve! actor "t4")

    (exec! actor "t5" {:op :actuation/issue-glazing-certificate :subject "batch-1"})
    (approve! actor "t5")

    ;; ---- batch-6 cover-glass full lifecycle ----
    (exec! actor "t6a" {:op :glazing-standard-rules/verify :subject "batch-6"})
    (approve! actor "t6a")
    (exec! actor "t6b" {:op :end-of-line-quality/screen :subject "batch-6"})
    (approve! actor "t6b")
    (exec! actor "t6c" {:op :robotics/simulate-flexural-strength-test :subject "batch-6"})
    (approve! actor "t6c")
    (exec! actor "t6d" {:op :actuation/ship-glass-panel-batch :subject "batch-6"})
    (approve! actor "t6d")
    (exec! actor "t6e" {:op :actuation/issue-glazing-certificate :subject "batch-6"})
    (approve! actor "t6e")

    ;; ---- HARD holds ----
    (exec! actor "t7" {:op :glazing-standard-rules/verify :subject "batch-2" :no-spec? true})

    (exec! actor "t8" {:op :glazing-standard-rules/verify :subject "batch-3"})
    (approve! actor "t8")
    (exec! actor "t8b" {:op :actuation/ship-glass-panel-batch :subject "batch-3"})
    (exec! actor "t8c" {:op :robotics/simulate-flexural-strength-test :subject "batch-3"})
    (approve! actor "t8c")
    (exec! actor "t9" {:op :actuation/ship-glass-panel-batch :subject "batch-3"})

    (exec! actor "t9b" {:op :glazing-standard-rules/verify :subject "batch-5"})
    (approve! actor "t9b")
    (exec! actor "t9c" {:op :actuation/ship-glass-panel-batch :subject "batch-5"})

    (exec! actor "t10" {:op :end-of-line-quality/screen :subject "batch-4"})

    (exec! actor "t11" {:op :actuation/ship-glass-panel-batch :subject "batch-1"})
    (exec! actor "t12" {:op :actuation/issue-glazing-certificate :subject "batch-1"})

    db))

;; ----------------------------- rendering -----------------------------

(defn- esc [v]
  (-> (str v)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")))

(defn- last-fact-for
  "Every op in this domain keys its ledger fact's `:subject` on the
  glass-panel-batch id itself -- see `glassworks.operation` commit-fact /
  `glassworks.governor` hold-fact. So a per-batch ledger lookup reflects
  that batch's own most recent op outcome -- for batch-1 that is the
  REJECTED double-certificate attempt (t12), which is why the ground-
  truth Shipped/Certified columns below are read directly from the
  batch record (`:glass-batch-shipped?`/`:glazing-certified?`), not
  inferred from this last-fact status."
  [ledger subject-id]
  (last (filter #(= (:subject %) subject-id) ledger)))

(defn- status-cell [ledger subject-id]
  (let [f (last-fact-for ledger subject-id)]
    (cond
      (nil? f) "<span class=\"muted\">no activity</span>"
      (= :committed (:t f)) "<span class=\"ok\">committed</span>"
      (= :approval-granted (:t f)) "<span class=\"ok\">approved &amp; committed</span>"
      (= :governor-hold (:t f))
      (let [rules (map name (:basis f))]
        (str "<span class=\"critical\">HARD hold &middot; " (esc (str/join ", " rules)) "</span>"))
      (= :approval-requested (:t f)) "<span class=\"warn\">awaiting approval</span>"
      :else "<span class=\"muted\">in progress</span>")))

(defn- batch-row [ledger {:keys [id batch-name product-class jurisdiction
                                  flexural-test-pin-mass-kg panel-thickness-actual-mm
                                  sim-peak-flexural-stress-mpa
                                  flexural-strength-min-mpa flexural-strength-max-mpa
                                  thickness-deviation-actual-mm
                                  thickness-deviation-min-mm thickness-deviation-max-mm
                                  eol-defect-unresolved?
                                  robotics-sim-verified?
                                  glass-batch-shipped? glazing-certified?]}]
  (format (str "        <tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td>"
               "<td>%s kg / %s mm</td>"
               "<td>%s &isin; [%s,%s] MPa</td>"
               "<td>%s &isin; [%s,%s] mm</td>"
               "<td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>")
          (esc id) (esc batch-name)
          (esc (if (keyword? product-class) (name product-class) product-class))
          (esc jurisdiction)
          (esc flexural-test-pin-mass-kg) (esc panel-thickness-actual-mm)
          (esc sim-peak-flexural-stress-mpa)
          (esc flexural-strength-min-mpa) (esc flexural-strength-max-mpa)
          (esc thickness-deviation-actual-mm)
          (esc thickness-deviation-min-mm) (esc thickness-deviation-max-mm)
          (if eol-defect-unresolved?
            "<span class=\"err\">unresolved</span>"
            "<span class=\"ok\">resolved</span>")
          (if robotics-sim-verified? "<span class=\"ok\">yes</span>" "<span class=\"muted\">no</span>")
          (str (if glass-batch-shipped? "<span class=\"ok\">shipped</span>" "<span class=\"muted\">not shipped</span>")
               " / "
               (if glazing-certified? "<span class=\"ok\">certified</span>" "<span class=\"muted\">not certified</span>"))
          (status-cell ledger id)))

(defn- ledger-row [{:keys [t op subject disposition basis]}]
  (format "        <tr><td>%s</td><td><code>%s</code></td><td>%s</td><td>%s</td></tr>"
          (esc (name t)) (esc (name (or op :n-a))) (esc subject)
          (esc (or (some->> basis (map name) (str/join ", ")) (some-> disposition name) ""))))

(defn- draft-row [kind {:strs [record_id batch_id jurisdiction]}]
  (format "        <tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>"
          (esc record_id) (esc kind) (esc batch_id) (esc jurisdiction)))

(def ^:private action-gate-rows
  ;; Static description of this actor's own op contract (README / phase /
  ;; governor) -- documentation of fixed behavior, not runtime telemetry.
  ["        <tr><td><code>:glass-panel-batch/intake</code></td><td><span class=\"ok\">auto-commit when clean, phase-3 (the ONLY phase-3 auto op)</span></td></tr>"
   "        <tr><td><code>:glazing-standard-rules/verify</code></td><td><span class=\"warn\">human approval (phase-gated, never auto-eligible) &middot; spec-basis independently re-checked -- no glazing-standard rules ever fabricated</span></td></tr>"
   "        <tr><td><code>:end-of-line-quality/screen</code></td><td><span class=\"critical\">unresolved optical/edge defect is a HARD, un-overridable hold</span></td></tr>"
   "        <tr><td><code>:robotics/simulate-flexural-strength-test</code></td><td><span class=\"warn\">human approval &middot; runs the REAL ASTM C158 flexural bend-test-cell <code>physics-2d</code> mission</span></td></tr>"
   "        <tr><td><code>:actuation/ship-glass-panel-batch</code></td><td><span class=\"warn\">ALWAYS human approval (safety-critical, regardless of confidence) &middot; evidence/robotics-sim/thickness independently re-checked &middot; double-shipment blocked</span></td></tr>"
   "        <tr><td><code>:actuation/issue-glazing-certificate</code></td><td><span class=\"warn\">ALWAYS human approval (safety-critical) &middot; end-of-line defect independently re-checked &middot; double-issuance blocked</span></td></tr>"])

(defn render
  "Renders the full operator-console.html document from a store `db`
  that has already run `run-demo!` (or any other real scenario)."
  [db]
  (let [ledger (vec (store/ledger db))
        batches (store/all-batches db)
        batch-rows (str/join "\n" (map (partial batch-row ledger) batches))
        ledger-rows (str/join "\n" (map ledger-row ledger))
        shipment-rows (str/join "\n" (map (partial draft-row "glass-panel-batch-shipment-draft")
                                          (store/shipment-history db)))
        certificate-rows (str/join "\n" (map (partial draft-row "glazing-certificate-draft")
                                             (store/evidence-history db)))]
    (str
     "<html><head><meta charset=\"utf-8\"><title>cloud-itonami-isic-2310 &middot; flat-glass plant</title><style>"
     (jp-go-dds.skin/dds+skin)
     "</style></head><body>\n"
     "<header class=\"bar\">\n"
     "  <h1>Flat-glass plant (ISIC 2310) — Operator Console</h1>\n"
     "  <span class=\"badge\">read-only sample · governor-gated · shipment/Glazing-Certificate actuation always human</span>\n"
     "</header>\n"
     "<main>\n"
     "  <section class=\"card\">\n"
     "    <h2>Glass-panel batches</h2>\n"
     "    <p class=\"muted\">Demo snapshot — build-time-generated from <code>glassworks.store</code> via <code>glassworks.render-html</code> (<code>clojure -M:dev:render-html</code>), regenerated nightly. Flexural-stress bounds (real <code>physics-2d</code>-simulated telemetry, ADR-2607151600/ADR-2607152000), thickness deviation, and Shipped/Certified are ground truth the governor independently re-derives — never trusted from a proposal's own report.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Batch</th><th>Name</th><th>Product class</th><th>Jurisdiction</th><th>Pin mass / thickness</th><th>Peak flexural stress</th><th>Thickness deviation</th><th>EOL</th><th>Robotics sim on file</th><th>Shipped / Certified</th><th>Last op status</th></tr></thead>\n"
     "      <tbody>\n"
     batch-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Action gate (Tempering Governor)</h2>\n"
     "    <p class=\"muted\">HARD holds cannot be overridden by any phase or human approval. Glass-panel-batch shipment and Glazing Certificate actuation are always a human glazing quality engineer's call, at every phase.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Op</th><th>Gate</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" action-gate-rows) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Draft shipment / Glazing Certificate records</h2>\n"
     "    <p class=\"muted\">Unsigned drafts (<code>glassworks.registry</code>) — the plant's own signature/submission is a separate, later act, never performed by this actor.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Record id</th><th>Kind</th><th>Batch</th><th>Jurisdiction</th></tr></thead>\n"
     "      <tbody>\n"
     shipment-rows "\n"
     certificate-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Audit ledger (this run)</h2>\n"
     "    <p class=\"muted\">Append-only decision-fact log — every proposal, hold and commit this scenario produced.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Fact</th><th>Op</th><th>Subject</th><th>Basis</th></tr></thead>\n"
     "      <tbody>\n"
     ledger-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "</main>\n"
     "</body></html>\n")))

(defn -main [& args]
  (let [out (or (first args) "docs/samples/operator-console.html")
        db (run-demo!)
        html (render db)]
    (spit out html)
    (println "wrote" out "(" (count (store/ledger db)) "ledger facts,"
             (count (store/shipment-history db)) "shipment drafts,"
             (count (store/evidence-history db)) "certificate drafts )")))
