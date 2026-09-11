(ns glassworks.phase
  "Phase 0->3 staged rollout -- the flat-glass-manufacturer analog of
  `automotive.phase`/`bodyshop.phase`.

    Phase 0  read-only        -- no writes, still governor-gated.
    Phase 1  assisted-intake  -- glass-panel-batch intake allowed,
                                 every write needs human approval.
    Phase 2  assisted-verify  -- adds glazing-standard requirements
                                 verification + end-of-line quality
                                 screening + robot flexural-bend-test
                                 simulation writes, still approval.
    Phase 3  supervised auto  -- governor-clean, high-confidence
                                 `:glass-panel-batch/intake` (no
                                 capital risk yet) may auto-commit.
                                 `:actuation/ship-glass-panel-batch`/
                                 `:actuation/issue-glazing-certificate`
                                 NEVER auto-commit, at any phase.

  `:actuation/ship-glass-panel-batch`/`:actuation/issue-glazing-
  certificate` are deliberately ABSENT from every phase's `:auto` set,
  including phase 3 -- a permanent structural fact, not a rollout
  milestone still to come. Shipping a real glass-panel batch onward to
  a downstream consumer and issuing a real Glazing Certificate are the
  two real-world legal acts this actor performs; both are always a
  human quality-engineer's call. `glassworks.governor`'s
  `:actuation/ship-glass-panel-batch`/`:actuation/issue-glazing-
  certificate` high-stakes gate enforces the same invariant
  independently -- two layers, not one, agree on this.
  `:end-of-line-quality/screen`/`:robotics/simulate-flexural-strength-
  test` are likewise never auto-eligible, at any phase -- the same
  posture every sibling's screening/verification op has.
  Phase 3's `:auto` set here has only ONE member
  (`:glass-panel-batch/intake`) -- this domain has no separate
  no-capital-risk 'file' lifecycle distinct from the batch record
  itself.")

(def read-ops  #{})
(def write-ops #{:glass-panel-batch/intake :glazing-standard-rules/verify :end-of-line-quality/screen
                 :robotics/simulate-flexural-strength-test
                 :actuation/ship-glass-panel-batch :actuation/issue-glazing-certificate})

;; NOTE the invariant: `:actuation/ship-glass-panel-batch`/
;; `:actuation/issue-glazing-certificate` are members of `write-ops`
;; (governor-gated like any write) but are NEVER members of any
;; phase's `:auto` set below. Do not add them there.
(def phases
  "phase -> {:label .. :writes <ops allowed to write> :auto <ops allowed to
  auto-commit when governor-clean>}."
  {0 {:label "read-only"        :writes #{}                                                          :auto #{}}
   1 {:label "assisted-intake"  :writes #{:glass-panel-batch/intake}                                  :auto #{}}
   2 {:label "assisted-verify"  :writes #{:glass-panel-batch/intake :glazing-standard-rules/verify :end-of-line-quality/screen
                                          :robotics/simulate-flexural-strength-test}          :auto #{}}
   3 {:label "supervised-auto"  :writes write-ops
      :auto #{:glass-panel-batch/intake}}})

(def default-phase 3)

(defn gate
  "Adjust a governor disposition for the rollout phase. Returns
  {:disposition kw :reason kw|nil}.

  - a governor HOLD always stays HOLD (compliance wins).
  - a write op not yet enabled in this phase -> HOLD (:phase-disabled).
  - a write op enabled but not auto-eligible -> ESCALATE (:phase-approval),
    even if the governor was clean.
  - `:actuation/ship-glass-panel-batch`/`:actuation/issue-glazing-
    certificate` are never auto-eligible at any phase, so they always
    escalate once the governor clears them (or hold if the governor
    doesn't)."
  [phase {:keys [op]} governor-disposition]
  (let [{:keys [writes auto]} (get phases phase (get phases default-phase))]
    (cond
      (= :hold governor-disposition)       {:disposition :hold :reason nil}
      (contains? read-ops op)              {:disposition governor-disposition :reason nil}
      (not (contains? writes op))          {:disposition :hold :reason :phase-disabled}
      (and (= :commit governor-disposition)
           (not (contains? auto op)))      {:disposition :escalate :reason :phase-approval}
      :else                                {:disposition governor-disposition :reason nil})))

(defn verdict->disposition
  "Map a Tempering Governor verdict to a base disposition before the
  phase gate."
  [verdict]
  (cond (:hard? verdict) :hold
        (:escalate? verdict) :escalate
        :else :commit))
