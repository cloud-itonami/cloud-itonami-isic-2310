(ns glassworks.registry
  "Pure-function glass-panel-batch-shipment + Glazing/Glass Test
  Certificate record construction -- an append-only glass
  manufacturer book-of-record draft.

  Like every sibling actor's registry, there is no single
  international check-digit standard for a batch-shipment or Glazing
  Certificate reference number -- every manufacturer/jurisdiction
  assigns its own reference format. This namespace does NOT invent
  one; it builds a jurisdiction-scoped sequence number and validates
  the record's required fields, the same honest, non-fabricating
  discipline `glassworks.facts` uses.

  `panel-thickness-deviation-out-of-range?` is this fleet's two-sided
  range check family, applying the SAME lo/hi bounds-comparison shape
  established by `testlab.registry/within-tolerance?` /
  `conservation.registry/body-condition-out-of-range?` /
  `water.registry/contaminant-level-out-of-range?` /
  `steelworks.registry/heat-chemistry-out-of-range?` /
  `turbine.registry/unit-tolerance-out-of-range?` /
  `automotive.registry/vehicle-emissions-out-of-range?` to a glass-
  panel-batch's own measured thickness deviation against the batch's
  own recorded spec bounds -- a SEPARATE ground-truth check from the
  robotics-simulated flexural-strength reading (`glassworks.robotics`
  handles that one).

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real plant/furnace/tempering-line control system. It
  builds the RECORD a manufacturer would keep, not the act of
  shipping the batch or issuing the Glazing Certificate itself (that
  is `glassworks.operation`'s `:actuation/ship-glass-panel-batch`/
  `:actuation/issue-glazing-certificate`, always human-gated -- see
  README `Actuation`)."
  (:require [clojure.string :as str]))

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is
  the manufacturer's own act, not this actor's. See README `Actuation`."
  [kind subject record-id]
  {"@context" ["https://www.w3.org/ns/credentials/v2"]
   "type" ["VerifiableCredential" kind]
   "credentialSubject" {"id" subject "record" record-id}
   "proof" nil
   "issued_by_registry" false
   "status" "draft-unsigned"})

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

(defn panel-thickness-deviation-out-of-range?
  "Does `batch`'s own `:thickness-deviation-actual-mm` fall outside
  its own `[:thickness-deviation-min-mm :thickness-deviation-max-mm]`
  recorded spec bounds? A pure ground-truth check against the batch's
  own permanent fields -- no upstream comparison needed. One of this
  fleet's two-sided range check family (see ns docstring)."
  [{:keys [thickness-deviation-actual-mm thickness-deviation-min-mm thickness-deviation-max-mm]}]
  (and (number? thickness-deviation-actual-mm) (number? thickness-deviation-min-mm) (number? thickness-deviation-max-mm)
       (or (< thickness-deviation-actual-mm thickness-deviation-min-mm)
           (> thickness-deviation-actual-mm thickness-deviation-max-mm))))

(defn- downstream-consumer
  "The REAL downstream repo a shipped glass-panel-batch feeds, keyed
  off its own recorded `:product-class` -- see README `Scope note`.
  Never invented per-shipment; the two-member mapping is fixed and
  disclosed."
  [product-class]
  (case product-class
    :automotive-safety-glazing ["cloud-itonami-isic-2910" "cloud-itonami-isic-2920"]
    :cover-glass ["cloud-itonami-isic-2630"]
    []))

(defn register-glass-panel-batch-shipment
  "Validate + construct the GLASS-PANEL-BATCH-SHIPMENT registration
  DRAFT -- the manufacturer's own act of dispatching a finished glass-
  panel batch onward to a downstream consumer (automotive glazing ->
  `cloud-itonami-isic-2910`/`-2920`, cover glass ->
  `cloud-itonami-isic-2630`). Pure function -- does not touch any real
  plant/logistics control system; it builds the RECORD a manufacturer
  would keep. `glassworks.governor` independently re-verifies the
  batch's own thickness-deviation sufficiency against its own spec
  bounds, and a double-shipment for the same batch, before this is
  ever allowed to commit."
  [batch-id jurisdiction sequence product-class]
  (when-not (and batch-id (not= batch-id ""))
    (throw (ex-info "glass-panel-batch-shipment: batch_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "glass-panel-batch-shipment: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "glass-panel-batch-shipment: sequence must be >= 0" {})))
  (let [shipment-number (str (str/upper-case jurisdiction) "-GLZ-" (zero-pad sequence 6))
        record {"record_id" shipment-number
                "kind" "glass-panel-batch-shipment-draft"
                "batch_id" batch-id
                "jurisdiction" jurisdiction
                "downstream_consumers" (downstream-consumer product-class)
                "immutable" true}]
    {"record" record "shipment_number" shipment-number
     "certificate" (unsigned-certificate "GlassPanelBatchShipment" shipment-number shipment-number)}))

(defn register-glazing-certificate
  "Validate + construct the GLAZING/GLASS-TEST-CERTIFICATE
  registration DRAFT -- the manufacturer's own act of issuing a real
  Glazing/Glass Test Certificate certifying a batch as conforming to
  the applicable glazing/glass-test standard (ANSI/SAE Z26.1, UNECE
  R43, JIS R 3211 or ASTM C158, per `glassworks.facts`). Pure function
  -- does not touch any real plant/tempering-line control system; it
  builds the RECORD a manufacturer would keep. `glassworks.governor`
  independently re-verifies the batch's own end-of-line defect
  resolution status, and a double-issuance for the same batch, before
  this is ever allowed to commit."
  [batch-id jurisdiction sequence]
  (when-not (and batch-id (not= batch-id ""))
    (throw (ex-info "glazing-certificate: batch_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "glazing-certificate: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "glazing-certificate: sequence must be >= 0" {})))
  (let [evidence-number (str (str/upper-case jurisdiction) "-GTC-" (zero-pad sequence 6))
        record {"record_id" evidence-number
                "kind" "glazing-certificate-draft"
                "batch_id" batch-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "evidence_number" evidence-number
     "certificate" (unsigned-certificate "GlazingCertificate" evidence-number evidence-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
