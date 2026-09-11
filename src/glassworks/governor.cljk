(ns glassworks.governor
  "Tempering Governor -- the independent compliance layer that earns
  the Glass Advisor the right to commit. The LLM has no notion of
  glazing-standard law, whether a glass-panel batch's own measured
  thickness deviation actually stays within its own recorded spec
  bounds, whether an end-of-line-detected optical/edge defect against
  the batch has actually stayed unresolved, or when an act stops
  being a draft and becomes a real-world glass-panel-batch shipment or
  Glazing Certificate issuance, so this MUST be a separate system able
  to *reject* a proposal and fall back to HOLD -- the flat-glass-
  manufacturer analog of `automotive.governor`/`bodyshop.governor`.

  Seven checks, in priority order, ALL HARD violations: a human approver
  CANNOT override them (you don't get to approve your way past a
  fabricated glazing-standard spec-basis, incomplete evidence, a robot
  flexural-bend-test simulation that never ran or that independently
  re-checks out-of-tolerance, an out-of-spec batch, an unresolved
  end-of-line defect, or a double shipment/certificate-issuance). The
  confidence/actuation gate is SOFT: it asks a human to look (low
  confidence / actuation), and the human may approve -- but see
  `glassworks.phase`: for `:stake :actuation/ship-glass-panel-batch`/
  `:actuation/issue-glazing-certificate` (a real safety-critical act)
  NO phase ever allows auto-commit either. Two independent layers agree
  that actuation is always a human call.

    1. Spec-basis                  -- did the requirements proposal cite
                                       an OFFICIAL/industry source
                                       (`glassworks.facts`), or invent one?
    2. Evidence incomplete         -- for `:actuation/ship-glass-panel-
                                       batch`/`:actuation/issue-glazing-
                                       certificate`, has the batch actually
                                       been verified with the jurisdiction/
                                       product-class's full flexural-
                                       strength-test-report/flat-glass-
                                       baseline-conformance-report/
                                       fragmentation-or-penetration-test-
                                       report/material-certification-record
                                       evidence checklist on file?
    3. Robot flexural-bend-test
       simulation missing or
       independently out-of-
       tolerance                    -- for `:actuation/ship-glass-panel-
                                       batch`, has the robot flexural-bend-
                                       test verification mission
                                       (`glassworks.robotics`) actually run
                                       and been recorded on the batch
                                       (`:robotics-sim-verified?`)? AND
                                       INDEPENDENTLY recompute whether the
                                       batch's own recorded REAL
                                       physics-2d-simulated flexural-stress
                                       reading falls out of its own recorded
                                       tolerance bounds
                                       (`glassworks.robotics/
                                       simulation-out-of-tolerance?`),
                                       ignoring whatever :passed? verdict
                                       the mission run itself stored -- the
                                       same 'ground truth, not self-report'
                                       discipline check 4 below uses for
                                       thickness deviation.
    4. Panel thickness deviation
       out of range                  -- for `:actuation/ship-glass-panel-
                                       batch`, INDEPENDENTLY recompute
                                       whether the batch's own measured
                                       thickness deviation falls outside
                                       its own recorded spec bounds
                                       (`glassworks.registry/panel-
                                       thickness-deviation-out-of-range?`)
                                       -- needs no proposal inspection or
                                       stored-verdict lookup at all. One of
                                       this fleet's two-sided range check
                                       family (`automotive.registry/
                                       vehicle-emissions-out-of-range?` and
                                       priors established the pattern;
                                       `glassworks.robotics/flexural-
                                       stress-out-of-tolerance?` above is a
                                       sibling instance).
    5. End-of-line defect unresolved -- reported by THIS proposal itself
                                       (an `:end-of-line-quality/screen`
                                       that just found an unresolved
                                       defect), or already on file for the
                                       batch (`:end-of-line-quality/
                                       screen`/`:actuation/issue-glazing-
                                       certificate`). Evaluated
                                       UNCONDITIONALLY (not scoped to a
                                       specific op), the SAME discipline
                                       every prior sibling governor
                                       establishes -- exercised in
                                       tests/demo via `:end-of-line-
                                       quality/screen` DIRECTLY, not via
                                       an actuation op against an
                                       unscreened batch -- see this ns's
                                       own test suite.
    6. Confidence floor / actuation
       gate                          -- LLM confidence below threshold,
                                       OR the op is `:actuation/ship-
                                       glass-panel-batch`/`:actuation/
                                       issue-glazing-certificate` (REAL
                                       safety-critical acts) -> escalate.

  Two more guards, double-shipment/double-certificate-issuance
  prevention, are enforced but NOT listed as numbered HARD checks
  above because they need no upstream comparison at all --
  `already-shipped-violations`/`already-certified-violations` refuse
  to ship a batch/issue a Glazing Certificate for the SAME batch
  twice, off dedicated `:glass-batch-shipped?`/`:glazing-certified?`
  facts (never a `:status` value) -- the SAME 'check a dedicated
  boolean, not status' discipline every prior sibling governor's
  guards establish, informed by `cloud-itonami-isic-6492`'s status-
  lifecycle bug (ADR-2607071320)."
  (:require [glassworks.facts :as facts]
            [glassworks.registry :as registry]
            [glassworks.robotics :as robotics]
            [glassworks.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Shipping a real glass-panel batch onward to a downstream consumer
  and issuing a real Glazing Certificate are the two real-world
  actuation events this actor performs -- a two-member set, matching
  every prior dual-actuation sibling's shape."
  #{:actuation/ship-glass-panel-batch :actuation/issue-glazing-certificate})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:glazing-standard-rules/verify` (or actuation) proposal with no
  spec-basis citation is a HARD violation -- never invent a
  jurisdiction/product-class's glazing-standard requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:glazing-standard-rules/verify :actuation/ship-glass-panel-batch :actuation/issue-glazing-certificate} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式/業界spec-basisの引用が無い提案はglazing-standard要件として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:actuation/ship-glass-panel-batch`/`:actuation/issue-glazing-
  certificate`, the jurisdiction/product-class's required flexural-
  strength-test-report/flat-glass-baseline-conformance-report/
  fragmentation-or-penetration-test-report/material-certification-
  record evidence must actually be satisfied -- do not trust the
  advisor's self-reported confidence alone."
  [{:keys [op subject]} st]
  (when (contains? #{:actuation/ship-glass-panel-batch :actuation/issue-glazing-certificate} op)
    (let [a (store/batch st subject)
          verification (store/requirements-verification-of st subject)]
      (when-not (and verification
                     (facts/required-evidence-satisfied?
                      (:jurisdiction a) (:checklist verification)))
        [{:rule :evidence-incomplete
          :detail "法域/製品クラスの必要書類(曲げ強度試験報告書/フラットガラス基礎適合報告書/破砕性試験報告書/材料証明記録等)が充足していない状態での提案"}]))))

(defn- robotics-simulation-violations
  "For `:actuation/ship-glass-panel-batch`: HARD hold if the robot
  flexural-bend-test verification mission (`glassworks.robotics`)
  never ran and was recorded on the batch (`:robotics-sim-verified?`),
  OR if it did but an INDEPENDENT recompute of the batch's own
  recorded REAL physics-2d-simulated flexural-stress reading
  (`glassworks.robotics/simulation-out-of-tolerance?`) says
  out-of-tolerance right now -- never trusts the mission's own stored
  :passed? verdict alone, the same discipline `panel-thickness-
  deviation-out-of-range-violations` below uses for thickness
  deviation."
  [{:keys [op subject]} st]
  (when (= op :actuation/ship-glass-panel-batch)
    (let [a (store/batch st subject)]
      (cond
        (not (:robotics-sim-verified? a))
        [{:rule :robotics-simulation-missing
          :detail (str subject " の曲げ強度試験ロボット検証ミッションが未実行・未合格")}]

        (robotics/simulation-out-of-tolerance? a)
        [{:rule :robotics-simulation-out-of-tolerance
          :detail (str subject " の実測曲げ応力("
                       (:sim-peak-flexural-stress-mpa a) " MPa)が独立再検証で許容範囲["
                       (:flexural-strength-min-mpa a) "," (:flexural-strength-max-mpa a) "]MPaを逸脱")}]))))

(defn- panel-thickness-deviation-out-of-range-violations
  "For `:actuation/ship-glass-panel-batch`, INDEPENDENTLY recompute
  whether the batch's own thickness deviation falls outside its own
  recorded spec bounds via `glassworks.registry/panel-thickness-
  deviation-out-of-range?` -- needs no proposal inspection or
  stored-verdict lookup at all, since its inputs are permanent
  ground-truth fields already on the batch."
  [{:keys [op subject]} st]
  (when (= op :actuation/ship-glass-panel-batch)
    (let [a (store/batch st subject)]
      (when (registry/panel-thickness-deviation-out-of-range? a)
        [{:rule :panel-thickness-deviation-out-of-range
          :detail (str subject " の実測厚み偏差(" (:thickness-deviation-actual-mm a)
                      "mm)が仕様範囲[" (:thickness-deviation-min-mm a) "," (:thickness-deviation-max-mm a) "]mmを逸脱")}]))))

(defn- end-of-line-defect-unresolved-violations
  "An unresolved end-of-line-detected optical/edge/inclusion defect --
  reported by THIS proposal (e.g. an `:end-of-line-quality/screen`
  that itself just found one), or already on file in the store for
  the batch (`:end-of-line-quality/screen`/`:actuation/issue-glazing-
  certificate`) -- is a HARD, un-overridable hold. Evaluated
  UNCONDITIONALLY (not scoped to a specific op) so the screening op
  itself can HARD-hold on its own finding."
  [{:keys [op subject]} proposal st]
  (let [hit-in-proposal? (= :unresolved (get-in proposal [:value :verdict]))
        batch-id (when (contains? #{:end-of-line-quality/screen :actuation/issue-glazing-certificate} op) subject)
        hit-on-file? (and batch-id (= :unresolved (:verdict (store/eol-screen-of st batch-id))))]
    (when (or hit-in-proposal? hit-on-file?)
      [{:rule :end-of-line-defect-unresolved
        :detail "未解決の完成検査欠陥がある状態でのガラス試験証明書発行提案は進められない"}])))

(defn- already-shipped-violations
  "For `:actuation/ship-glass-panel-batch`, refuses to ship the SAME
  batch twice, off a dedicated `:glass-batch-shipped?` fact (never a
  `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :actuation/ship-glass-panel-batch)
    (when (store/batch-already-shipped? st subject)
      [{:rule :already-shipped
        :detail (str subject " は既に出荷済み")}])))

(defn- already-certified-violations
  "For `:actuation/issue-glazing-certificate`, refuses to issue a
  Glazing Certificate for the SAME batch twice, off a dedicated
  `:glazing-certified?` fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :actuation/issue-glazing-certificate)
    (when (store/batch-already-certified? st subject)
      [{:rule :already-certified
        :detail (str subject " は既にガラス試験証明書発行済み")}])))

(defn check
  "Censors a Glass Advisor proposal against the Tempering Governor
  rules. Returns {:ok? bool :violations [..] :confidence c :escalate?
  bool :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (robotics-simulation-violations request st)
                           (panel-thickness-deviation-out-of-range-violations request st)
                           (end-of-line-defect-unresolved-violations request proposal st)
                           (already-shipped-violations request st)
                           (already-certified-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
