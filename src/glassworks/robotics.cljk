(ns glassworks.robotics
  "Robot-executed flexural-strength (ASTM C158) bend-test verification
  -- the concrete, actor-level realization of ADR-2607011000's
  robotics premise (every cloud-itonami vertical is designed on the
  premise that a robot performs the physical-domain work; an
  independent governor gates any action before it ever reaches
  hardware), delivered DIRECTLY onto ADR-2607152000's real-physics
  fleet extension (this vertical, isic-2310, is a NEW actor built to
  that same standard from day one, mirroring how
  `cloud-itonami-isic-2920`/`cloud-itonami-isic-2930`/
  `cloud-itonami-isic-2394` deliver it natively rather than
  retrofitted) for THIS actor's own manufacturing-process evidence
  requirement: a glass-panel-batch-shipment proposal must cite a real
  flexural-strength-test report actually on file -- not merely a
  self-reported checklist string.

  A genuine time-stepped `kotoba-lang/physics-2d` rigid-body
  simulation of an ASTM C158 three-point flexural (bend) strength
  test: a loading-pin `Body2D` (the test rig's moving loading nose)
  closes at a controlled velocity onto a static (mass 0, immovable --
  the same `cementmill.robotics`/`bodyshop.robotics`/`autoparts.
  robotics` specimen/fixture pattern) glass-panel specimen `Body2D`,
  supported at its ends on the test rig's fixed anvils. `world-step`
  actually integrates/collides/resolves the contact over real ticks;
  `:sim-peak-flexural-force-n` is read directly off the ACTUAL
  simulated velocity trajectory (F = m*a, the SAME technique every
  real-physics sibling in this fleet uses), and
  `:sim-peak-flexural-stress-mpa` converts that force into a genuine
  textbook flexural (bending) stress reading via the standard
  rectangular-beam center-loading formula

      sigma = 3*F*L / (2*w*h^2)

  (F = force N, L = support span mm, w = specimen width mm, h =
  specimen thickness mm -- the classic center-point-loaded
  simply-supported rectangular-beam bending-stress identity: maximum
  bending moment M = F*L/4 at midspan, section modulus for a
  rectangular cross-section Z = w*h^2/6, sigma = M/Z = 3*F*L/(2*w*h^2).
  ASTM C158 itself specifies BOTH three-point and four-point loading
  geometries; this ns models the three-point/center-loading case,
  disclosed honestly -- a four-point rig's peak moment differs by a
  span-ratio-dependent constant, not modeled here), giving a reading
  directly comparable to the batch's own recorded
  `:flexural-strength-min-mpa`/`:flexural-strength-max-mpa` acceptance
  band.

  HONEST REINTERPRETATION: like `bodyshop.robotics`'s stamping press,
  a flexural bend test genuinely IS a closing/colliding event -- the
  loading pin descends and contacts the specimen -- so this ns needs
  no virtual-limit-boundary reframing (unlike `autoparts.robotics`'s
  pull test); it is the SAME collision shape `cementmill.robotics`'s
  compressive-strength press models, substituting a supported glass
  panel for a cube specimen.

  Disclosed engineering priors (this ns's own, not measured facts --
  same discipline as `bodyshop.robotics`'s `press-closing-velocity-
  mps`/`draw-depth-m` disclosures):

  - `loading-pin-closing-velocity-mps` is a disclosed ANALOG closing
    rate for the test rig's loading-nose actuator, not a literal
    transcription of any one universal-testing-machine's crosshead-
    speed curve (real ASTM bend-test crosshead speeds are typically
    very slow, mm/min, quasi-static -- `physics-2d`'s impulse resolver
    has NO progressive load-deflection stiffness model at all, so a
    literal quasi-static speed would never produce a resolvable
    impulse in a handful of ticks; this ns picks a faster ANALOG rate,
    the SAME disclosed choice every real-physics sibling in this fleet
    makes).
  - `specimen-deflection-at-failure-m` (the glass specimen's own
    real, small quasi-static deflection at the moment of brittle
    fracture -- glass is a brittle material with very little elastic
    deflection before failure, commonly a few mm or less for a coupon
    span in this size class) is this ns's own disclosed engineering
    estimate for the crush/travel distance used to derive `dt` (the
    per-tick timestep), the SAME principled-not-arbitrary identity
    `cementmill.robotics`/`bodyshop.robotics`/`commsdevice.robotics`
    use for their own `dt`.
  - `support-span-mm` (100 mm) and `specimen-width-mm` (50 mm) are a
    DISCLOSED representative flexural-test coupon geometry -- ASTM
    C158 permits a range of specimen/span sizes depending on the
    product under test; this ns picks a plausible mid-range coupon
    geometry, not a literal reproduction of one specific certified lab
    fixture.
  - By exact kinematic identity (a = v^2/d for a boxcar full stop over
    transit distance d at speed v), `loading-pin-mass-kg` is the ONLY
    quantity that scales `:sim-peak-flexural-force-n`/`:sim-peak-
    flexural-stress-mpa` for a fixed closing velocity/deflection --
    the peak deceleration itself is INDEPENDENT of the loading-pin's
    own mass when colliding with a mass-0 (immovable) specimen (mass
    cancels algebraically in `physics-2d`'s `resolve-contact`, the
    SAME verified property every real-physics sibling in this fleet
    establishes). `specimen-thickness-mm` (this batch's OWN recorded
    panel thickness, h in the stress formula) is the SECOND quantity
    that scales the derived STRESS reading (via 1/h^2) even though it
    does not change the simulated FORCE itself -- a genuinely thinner
    panel under the SAME simulated force reads a genuinely higher
    flexural stress, exactly matching real beam-bending physics.

  `flexural-strength-min-mpa`/`flexural-strength-max-mpa` (the
  acceptance band `flexural-stress-out-of-tolerance?` checks against)
  is seeded per product class on the batch record itself
  (`glassworks.store`'s demo data), anchored against REAL published
  glass modulus-of-rupture ranges, disclosed honestly by confidence
  level:
    - annealed soda-lime float glass: commonly cited ~40-50 MPa
      typical flexural (modulus-of-rupture) strength -- a REASONABLY
      WELL-ESTABLISHED range in glass-engineering literature (ASTM
      E1300 / glass design references cite comparable figures). This
      ns is CONFIDENT in this range as a general order-of-magnitude
      anchor.
    - automotive TEMPERED safety glazing: commonly cited in the
      ~150 MPa+ range (tempering raises surface compressive stress,
      multiplying the apparent flexural strength several-fold over
      annealed glass) -- this ns treats 150 MPa as a REASONED
      ENGINEERING ESTIMATE floor for tempered automotive glazing, not
      a single verbatim standards-body citation; real production
      tempered-glass flexural strength varies with tempering process
      control and is commonly reported across a wide band above this
      floor.
    - chemically-strengthened cover glass: considerably higher than
      tempered glass -- ion-exchange strengthening produces a deep
      compressive layer that display-glass literature commonly
      associates with several-hundred-MPa-class flexural/modulus-of-
      rupture performance. This ns's ~450-900 MPa band is a REASONED
      ENGINEERING ESTIMATE (LOWER confidence than the annealed anchor
      above), disclosed as such -- not a single confidently-sourced
      citation, the same honesty discipline `commsdevice.robotics`'s
      OCA-bonding-pressure band and the retail drop-test ADR's 400g
      ceiling both use.

  Pure data + pure functions -- no real robot I/O, no network.
  `physics-2d/world-step` is itself a pure, fixed-timestep integrator
  (no wall-clock/IO), so this stays exactly as offline/deterministic
  as every other sibling namespace in this actor -- tests and the demo
  run without a network.

  Honest scope (mirrors every real-physics sibling's own disclosure):
  this DOES model a real time-stepped `physics-2d` rigid-body
  trajectory for the bend-test loading event, and DOES apply a genuine
  textbook flexural-stress formula to the resulting force. It does NOT
  model: glass material stiffness/stress-strain or a real force-vs-
  deflection curve (`physics-2d` has no such model at all), 3D
  geometry (2D projection only), the four-point loading geometry ASTM
  C158 also permits, a real load-cell/DAQ connection, or a real test-
  machine servo-motion-planning system -- still simulation, not
  control, the same 'policy, not control' boundary `kotoba.robotics`'s
  docstring already establishes."
  (:require [kotoba.robotics :as robotics]
            [physics-2d :as p2d]))

;; ---------------------------------------------------------------------------
;; Platform shims (mirrors physics-2d's/every real-physics sibling's own
;; private sqrt*/abs*/ceil* style, keeping this ns portable .cljc).
;; ---------------------------------------------------------------------------

(defn- abs* [x] (if (neg? x) (- x) x))

(defn- ceil* [x]
  #?(:clj  (Math/ceil (double x))
     :cljs (js/Math.ceil x)))

(def mission-actions
  "The three-step flexural-bend-test/quality-cell verification mission
  every glass-panel batch walks through before `:actuation/ship-glass-
  panel-batch` is proposable. :grasp/:actuate at :low safety, :sense
  at :none -- verification/QA handling of a stationary glass-panel
  specimen, not the moving-shipment actuation that is `:actuation/
  ship-glass-panel-batch` itself (always :safety-critical -- see
  `glassworks.governor`)."
  [{:step :specimen-loading-and-alignment    :kind :grasp   :safety :low}
   {:step :flexural-bend-test-cycle          :kind :actuate :safety :low}
   {:step :post-test-fracture-pattern-scan   :kind :sense   :safety :none}])

;; ---------------------- real, disclosed published anchors -------------------

(def flexural-strength-band-mpa
  "Real, published (range, disclosed-confidence) flexural-strength
  (modulus-of-rupture) acceptance bands per product class -- see ns
  docstring for the exact confidence disclosure each band carries.
  `:annealed` is a CONFIDENT general anchor (used only as the negative-
  control / under-tempered test fixture in this ns's own demo data --
  see `glassworks.store`); `:automotive-tempered` and `:cover-glass`
  are REASONED ENGINEERING ESTIMATES, lower confidence, disclosed as
  such."
  {:annealed            {:min 35.0  :max 55.0  :confidence :confident-general-anchor}
   :automotive-tempered {:min 150.0 :max 260.0 :confidence :reasoned-estimate}
   :cover-glass          {:min 450.0 :max 900.0 :confidence :reasoned-estimate}})

;; ---------------------- real physics-2d bend-test constants ----------------

(def ^:const loading-pin-closing-velocity-mps
  "Controlled loading-pin closing velocity (m/s) -- see ns docstring:
  a disclosed ANALOG rate for the bend-test rig's loading-nose
  actuator, not a literal transcription of any one universal-testing-
  machine's real (quasi-static, mm/min-class) crosshead-speed curve."
  0.2)

(def ^:const specimen-deflection-at-failure-m
  "Representative glass-specimen deflection at brittle fracture (m) --
  see ns docstring: a disclosed engineering estimate for the crush/
  travel distance used to derive `dt`, NOT a per-batch measured fact
  (glass is brittle; real elastic deflection before fracture in this
  coupon size class is small, on the order of a few mm)."
  0.002)

(def ^:const dt
  "Per-tick timestep (s) -- derived from THIS simulation's own
  specimen-deflection/closing-velocity (the nominal transit time
  across the specimen's own quasi-static deflection-to-failure zone),
  the SAME principled-not-arbitrary identity every real-physics
  sibling uses for its own `dt`."
  (/ specimen-deflection-at-failure-m loading-pin-closing-velocity-mps))

(def ^:const support-span-mm
  "Disclosed representative ASTM C158 three-point bend-test support
  span (mm) -- see ns docstring: ASTM C158 permits a range of
  specimen/span sizes; this is a plausible mid-range coupon geometry,
  not a literal reproduction of one specific certified lab fixture."
  100.0)

(def ^:const specimen-width-mm
  "Disclosed representative flexural-test coupon width (mm) -- the `w`
  term in `sigma = 3*F*L/(2*w*h^2)`."
  50.0)

(def ^:const loading-pin-half-w-m
  "Loading-pin AABB half-width (m) along the travel axis -- a thin,
  rigid loading nose; `physics-2d` colliders do not deform, so this
  dimension is a disclosed, arbitrary rigid-body stand-in, not a
  load-bearing physical parameter (mirrors `bodyshop.robotics`'s
  `die-half-w-m`)."
  0.005)

(def ^:const loading-pin-half-h-m
  "Loading-pin AABB half-height (m), lateral -- half of
  `specimen-width-mm` (converted to metres), a disclosed simplification
  so the whole modeled specimen width loads under the pin (physics-2d
  has no point/line-load collider primitive -- an honest, documented
  limitation shared with every AABB-modeled real-physics sibling in
  this fleet)."
  (/ (/ specimen-width-mm 1000.0) 2.0))

(defn- specimen-half-w-m
  "Glass-specimen AABB half-thickness (m) along the travel axis --
  THIS batch's own recorded `specimen-thickness-mm` (h in the stress
  formula), converted to metres and halved."
  [specimen-thickness-mm]
  (/ (/ specimen-thickness-mm 1000.0) 2.0))

(def ^:const specimen-half-h-m
  "Glass-specimen AABB half-height (m), lateral -- half of
  `specimen-width-mm`, the same lateral extent as the loading pin so
  the whole modeled width loads."
  loading-pin-half-h-m)

(def ^:const gap-m
  "Loading-pin standoff distance (m) the pin starts above the
  specimen, so the trajectory captures a real pre-contact approach
  phase, not just the collision tick itself (mirrors every sibling's
  own gap constant)."
  0.02)

(def ^:const settle-ticks
  "Extra ticks appended after the pin is expected to reach the
  specimen, so the trajectory also captures post-contact settling --
  the SAME constant + rationale as every real-physics sibling:
  `physics-2d`'s positional correction removes 80% of any remaining
  overlap per tick, so residual overlap after 15 more ticks is
  ~3e-11 of whatever it was at first contact."
  15)

;; ------------------------------ real simulation ------------------------------

(defn simulate-flexural-strength-test
  "Time-steps a REAL `physics-2d` world for ONE ASTM C158 three-point
  flexural bend-test cycle: a loading-pin `Body2D` (mass
  `loading-pin-mass-kg`, velocity `loading-pin-closing-velocity-mps`)
  approaches and collides with a static (mass 0, immovable -- the
  test rig's end-supported specimen) glass-panel-specimen `Body2D` of
  thickness `specimen-thickness-mm`. Returns {:trajectory [{:tick
  :position :velocity} ...] (pin body only) :sim-peak-flexural-force-n
  n :sim-peak-flexural-stress-mpa n :sim-peak-bend-travel-m n :ticks n
  :dt n :closing-velocity-mps n}.

  `:sim-peak-flexural-force-n` is `loading-pin-mass-kg` times the PEAK
  magnitude of tick-to-tick velocity change (along the travel axis)
  divided by `dt` -- F = m*a, derived from the ACTUAL simulated
  velocity trajectory. `:sim-peak-flexural-stress-mpa` converts that
  force into a genuine textbook flexural-stress reading via
  `sigma = 3*F*L/(2*w*h^2)` (L = `support-span-mm`, w =
  `specimen-width-mm`, h = `specimen-thickness-mm`) -- 1 MPa = 1
  N/mm^2 once L/w/h are all in mm and F in N -- so it is directly
  comparable to a batch's own recorded `:flexural-strength-min-mpa`/
  `:flexural-strength-max-mpa`. `:sim-peak-bend-travel-m` is the
  largest AABB penetration depth (m) actually observed between the
  pin's leading face and the specimen's near face across the whole
  trajectory -- informational, derived from the actual simulated
  positions, not invented.

  Pure, deterministic -- the same `loading-pin-mass-kg`/
  `specimen-thickness-mm` always reproduce the same telemetry; no IO,
  no wall-clock."
  [loading-pin-mass-kg specimen-thickness-mm]
  (let [v0 loading-pin-closing-velocity-mps
        spec-half-w (specimen-half-w-m specimen-thickness-mm)
        approach-m (+ gap-m loading-pin-half-w-m spec-half-w)
        ticks (long (+ settle-ticks (long (ceil* (/ approach-m (* v0 dt))))))
        specimen-x 0.0
        pin-x (- specimen-x spec-half-w loading-pin-half-w-m gap-m)
        pin (p2d/make-body {:position [pin-x 0.0]
                             :velocity [v0 0.0]
                             :mass (double loading-pin-mass-kg)
                             :restitution 0.0
                             :friction 0.0
                             :collider (p2d/make-aabb-collider loading-pin-half-w-m loading-pin-half-h-m)
                             :user-data :loading-pin})
        specimen (p2d/make-body {:position [specimen-x 0.0]
                                  :velocity [0.0 0.0]
                                  :mass 0.0
                                  :restitution 0.0
                                  :friction 0.0
                                  :collider (p2d/make-aabb-collider spec-half-w specimen-half-h-m)
                                  :user-data :glass-panel-specimen})
        w0 (p2d/world-new [0.0 0.0])
        [w1 pin-id] (p2d/world-add w0 pin)
        [w2 _spec-id] (p2d/world-add w1 specimen)
        worlds (reductions (fn [w _] (p2d/world-step w dt)) w2 (range ticks))
        trajectory (mapv (fn [tick world]
                            (let [b (nth (:bodies world) pin-id)]
                              {:tick tick :position (:position b) :velocity (:velocity b)}))
                          (range (count worlds)) worlds)
        vxs (mapv (comp first :velocity) trajectory)
        peak-decel-mps2 (->> (map (fn [va vb] (abs* (/ (- vb va) dt))) vxs (rest vxs))
                              (reduce max 0.0))
        contact-plane-x (- specimen-x spec-half-w)
        penetrations-m (mapv (fn [{:keys [position]}]
                                (max 0.0 (- (+ (first position) loading-pin-half-w-m) contact-plane-x)))
                              trajectory)
        peak-force-n (* (double loading-pin-mass-kg) peak-decel-mps2)
        peak-stress-mpa (/ (* 3.0 peak-force-n support-span-mm)
                            (* 2.0 specimen-width-mm specimen-thickness-mm specimen-thickness-mm))]
    {:trajectory trajectory
     :sim-peak-flexural-force-n peak-force-n
     :sim-peak-flexural-stress-mpa peak-stress-mpa
     :sim-peak-bend-travel-m (reduce max 0.0 penetrations-m)
     :ticks (count trajectory)
     :dt dt
     :closing-velocity-mps v0}))

(defn flexural-telemetry-for
  "Runs the REAL `simulate-flexural-strength-test` time-stepped
  `physics-2d` simulation for `batch`'s own recorded
  `:flexural-test-pin-mass-kg`/`:panel-thickness-actual-mm` press-run
  configuration and returns the actual simulated telemetry:
  {:sim-peak-flexural-force-n n :sim-peak-flexural-stress-mpa n
  :sim-peak-bend-travel-m n :ticks n :dt n :closing-velocity-mps n}.
  Pure, deterministic -- the same inputs always reproduce the same
  telemetry."
  [batch]
  (select-keys (simulate-flexural-strength-test
                (:flexural-test-pin-mass-kg batch)
                (:panel-thickness-actual-mm batch))
               [:sim-peak-flexural-force-n :sim-peak-flexural-stress-mpa
                :sim-peak-bend-travel-m :ticks :dt :closing-velocity-mps]))

(defn flexural-stress-out-of-tolerance?
  "Ground-truth check: does `batch`'s own recorded REAL
  `:sim-peak-flexural-stress-mpa` (the ACTUAL `physics-2d`-simulated
  bend-test reading -- see `flexural-telemetry-for`) fall outside its
  own recorded [:flexural-strength-min-mpa :flexural-strength-max-mpa]
  acceptance-band bounds? Reuses the batch's OWN already-established
  real acceptance-band fields (per product class, seeded from
  `flexural-strength-band-mpa` -- see ns docstring for the confidence
  disclosure on each band). Needs no mission run or proposal
  inspection once the telemetry is on file -- its inputs are permanent
  fields already on the batch, the same shape
  `glassworks.registry/panel-thickness-deviation-out-of-range?` uses
  for thickness deviation."
  [{:keys [sim-peak-flexural-stress-mpa flexural-strength-min-mpa flexural-strength-max-mpa]}]
  (and (number? sim-peak-flexural-stress-mpa) (number? flexural-strength-min-mpa) (number? flexural-strength-max-mpa)
       (or (< sim-peak-flexural-stress-mpa flexural-strength-min-mpa)
           (> sim-peak-flexural-stress-mpa flexural-strength-max-mpa))))

(defn simulate-flexural-test-cell
  "Run the robot flexural-bend-test verification mission for
  `batch-id` (`batch` is the full glass-panel-batch record, incl.
  `:flexural-test-pin-mass-kg` and `:flexural-strength-min-mpa`/
  `:flexural-strength-max-mpa`). Actually runs the REAL engine:
  `flexural-telemetry-for` -- the actual `physics-2d`-stepped loading-
  pin/glass-panel-specimen collision trajectory
  (`:sim-peak-flexural-force-n`/`:sim-peak-flexural-stress-mpa`).

  Returns {:mission .. :actions [{:action .. :proof ..} ..] :passed?
  bool :sim-peak-flexural-force-n n :sim-peak-flexural-stress-mpa n}.
  Deterministic: :passed? is derived from the batch's OWN recorded
  bend-test configuration via the REAL simulated trajectory
  (`flexural-stress-out-of-tolerance?`), never invented or randomized
  -- `kotoba.robotics` mandates no network/IO, and a repeatable
  simulation is what makes the governor's independent recheck
  (`simulation-out-of-tolerance?`) meaningful."
  [batch-id batch]
  (let [telemetry (flexural-telemetry-for batch)
        out-of-range? (flexural-stress-out-of-tolerance? (merge batch telemetry))
        reading (if out-of-range? :out-of-tolerance :nominal)
        mission (robotics/mission (str "mission-" batch-id "-flexural-bend-test")
                                   :robot/flexural-test-cell-1
                                   :flexural-strength-verification
                                   :boundaries {:station "glassworks-flexural-test-cell"}
                                   :max-steps (count mission-actions))
        actions (mapv (fn [{:keys [step kind safety]}]
                        (let [a (robotics/action (str (:mission/id mission) "-" (name step))
                                                  (:mission/id mission) kind safety
                                                  :params {:step step :batch-id batch-id})]
                          {:action a
                           :proof (robotics/telemetry-proof (:mission/id mission) step reading
                                                             :provenance :simulated)}))
                      mission-actions)]
    {:mission mission
     :actions actions
     :passed? (not out-of-range?)
     :sim-peak-flexural-force-n (:sim-peak-flexural-force-n telemetry)
     :sim-peak-flexural-stress-mpa (:sim-peak-flexural-stress-mpa telemetry)}))

(defn simulation-out-of-tolerance?
  "Independent ground-truth recheck for the governor: does `batch`'s
  OWN current, on-file real `physics-2d`-simulated flexural-stress
  telemetry (`:sim-peak-flexural-stress-mpa`) fall out of its own
  recorded acceptance band right now? Ignores whatever :passed?
  verdict a prior mission run stored -- identical in spirit to
  `glassworks.registry/panel-thickness-deviation-out-of-range?`'s
  refusal to trust a proposal's self-report."
  [batch]
  (flexural-stress-out-of-tolerance? batch))
