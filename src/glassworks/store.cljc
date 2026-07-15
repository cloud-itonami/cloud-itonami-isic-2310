(ns glassworks.store
  "SSoT for the glass-and-glass-products manufacturing actor, behind a
  `Store` protocol so the backend is a swap, not a rewrite -- the same
  seam every prior `cloud-itonami-isic-*` actor in this fleet uses:

    - `MemStore`     -- atom of EDN. The deterministic default for
                        dev/tests/demo (no deps).
    - `DatomicStore` -- backed by `langchain.db`, a Datomic-API-compatible
                        EAV store (datalog q / pull / upsert). Pure `.cljc`,
                        so it runs offline AND can be pointed at a real
                        Datomic Local or a kotoba-server pod by swapping
                        `langchain.db`'s `:db-api` (see langchain.kotoba-db).

  Both implement the same protocol and pass the same contract
  (test/glassworks/store_contract_test.clj), which is the whole point:
  the actor, the Tempering Governor and the audit ledger never know
  which SSoT they run on.

  Like `automotive.store`'s dual vehicle-dispatch/conformity-
  certificate history and `bodyshop.store`'s dual body-shell-shipment/
  quality-certificate history, this actor has TWO actuation events
  (shipping a glass-panel batch onward to a downstream consumer,
  issuing a Glazing/Glass Test Certificate) acting on the SAME entity
  (a glass-panel batch), each with its OWN history collection,
  sequence counter and dedicated double-actuation-guard boolean
  (`:glass-batch-shipped?`/`:glazing-certified?`, never a `:status`
  value) -- the same discipline every prior sibling governor's guards
  establish, informed by `cloud-itonami-isic-6492`'s status-lifecycle
  bug (ADR-2607071320).

  The ledger stays append-only on every backend: 'which batch was
  screened for an unresolved end-of-line optical/edge defect, which
  batch shipment was dispatched onward to a downstream consumer, which
  Glazing/Glass Test Certificate was issued, on what jurisdictional
  basis, approved by whom' is always a query over an immutable log --
  the audit trail a community trusting a flat-glass manufacturer
  needs, and the evidence a manufacturer needs if a shipment or
  certificate decision is later disputed."
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [glassworks.registry :as registry]
            [glassworks.robotics :as robotics]
            [langchain.db :as d]))

(defprotocol Store
  (batch [s id])
  (all-batches [s])
  (eol-screen-of [s batch-id] "committed end-of-line-defect screening verdict for a batch, or nil")
  (requirements-verification-of [s batch-id] "committed glazing-standard-rules evidence verification, or nil")
  (ledger [s])
  (shipment-history [s] "the append-only glass-panel-batch-shipment history (glassworks.registry drafts)")
  (evidence-history [s] "the append-only Glazing Certificate history (glassworks.registry drafts)")
  (next-shipment-sequence [s jurisdiction] "next shipment-number sequence for a jurisdiction")
  (next-evidence-sequence [s jurisdiction] "next evidence-number sequence for a jurisdiction")
  (batch-already-shipped? [s batch-id] "has this batch already been shipped onward?")
  (batch-already-certified? [s batch-id] "has this batch's Glazing Certificate already been issued?")
  (commit-record! [s record] "apply a committed op's record to the SSoT")
  (append-ledger! [s fact]   "append one immutable decision fact")
  (with-batches [s batches] "replace/seed the batch directory (map id->batch)"))

;; ----------------------------- demo data -----------------------------

(defn- with-flexural-telemetry
  "Merges REAL flexural-bend-test telemetry onto a demo batch's base
  fields -- `glassworks.robotics/flexural-telemetry-for` actually runs
  `simulate-flexural-strength-test`'s `physics-2d`-stepped simulation
  for this batch's own `:flexural-test-pin-mass-kg`/`:panel-thickness-
  actual-mm` (ADR-2607152000), so even the 'already on file' seed data
  (as if from an earlier real flexural-bend-test report) is genuinely
  simulation-derived, never hand-typed doubles."
  [base]
  (merge base (select-keys (robotics/flexural-telemetry-for base)
                           [:sim-peak-flexural-force-n :sim-peak-flexural-stress-mpa])))

(defn demo-data
  "A small, self-contained glass-panel-batch set covering both product
  classes (automotive safety glazing, cover glass -- see README `Scope
  note`) and both actuation lifecycles (shipping a batch onward to a
  downstream consumer, issuing a Glazing Certificate) so the actor +
  tests run offline.

  `:flexural-test-pin-mass-kg`/`:panel-thickness-actual-mm`
  (ADR-2607152000) are permanent batch bend-test-configuration fields
  (like `:overall-length-actual-mm` on other sibling actors);
  `:sim-peak-flexural-force-n`/`:sim-peak-flexural-stress-mpa` are the
  REAL `glassworks.robotics/simulate-flexural-strength-test`-computed
  telemetry for those fields. `batch-5` (a windshield lot) is
  DELIBERATELY recorded with a much lighter `:flexural-test-pin-mass-
  kg`/thicker `:panel-thickness-actual-mm` combination than its own
  recorded (automotive-tempered) `:flexural-strength-min-mpa` acceptance
  band requires -- a genuine UNDER-TEMPERED panel (someone/something
  shipped this lot's tempering-furnace configuration too mild, or
  logged the wrong bend-test rig configuration): the real, re-run
  simulated flexural stress (45.0 MPa, annealed-glass-level) genuinely
  falls below the 150.0 MPa automotive-tempered floor -- the ground
  truth `glassworks.robotics/simulation-out-of-tolerance?` independently
  rechecks, never trusting a prior mission's stored :passed? verdict
  alone."
  []
  {:batches
   {"batch-1" (with-flexural-telemetry
               {:id "batch-1" :batch-name "Sakura Float-Line Windshield Lot WS-04"
                :product-class :automotive-safety-glazing :jurisdiction "JPN"
                :panel-thickness-actual-mm 4.0 :flexural-test-pin-mass-kg 42.7
                :flexural-strength-min-mpa 150.0 :flexural-strength-max-mpa 260.0
                :thickness-deviation-actual-mm 0.05 :thickness-deviation-min-mm -0.10 :thickness-deviation-max-mm 0.10
                :eol-defect-unresolved? false
                :robotics-sim-verified? false :robotics-sim-record nil
                :glass-batch-shipped? false :glazing-certified? false
                :status :intake})
    "batch-2" (with-flexural-telemetry
               {:id "batch-2" :batch-name "Atlantis Windshield Lot WS-12"
                :product-class :automotive-safety-glazing :jurisdiction "ATL"
                :panel-thickness-actual-mm 4.0 :flexural-test-pin-mass-kg 42.7
                :flexural-strength-min-mpa 150.0 :flexural-strength-max-mpa 260.0
                :thickness-deviation-actual-mm 0.05 :thickness-deviation-min-mm -0.10 :thickness-deviation-max-mm 0.10
                :eol-defect-unresolved? false
                :robotics-sim-verified? false :robotics-sim-record nil
                :glass-batch-shipped? false :glazing-certified? false
                :status :intake})
    "batch-3" (with-flexural-telemetry
               {:id "batch-3" :batch-name "鈴木サイドライト Lot SL-07"
                :product-class :automotive-safety-glazing :jurisdiction "JPN"
                :panel-thickness-actual-mm 4.0 :flexural-test-pin-mass-kg 42.7
                :flexural-strength-min-mpa 150.0 :flexural-strength-max-mpa 260.0
                :thickness-deviation-actual-mm 0.35 :thickness-deviation-min-mm -0.10 :thickness-deviation-max-mm 0.10
                :eol-defect-unresolved? false
                :robotics-sim-verified? false :robotics-sim-record nil
                :glass-batch-shipped? false :glazing-certified? false
                :status :intake})
    "batch-4" (with-flexural-telemetry
               {:id "batch-4" :batch-name "田中バックライト Lot BL-03"
                :product-class :automotive-safety-glazing :jurisdiction "JPN"
                :panel-thickness-actual-mm 4.0 :flexural-test-pin-mass-kg 42.7
                :flexural-strength-min-mpa 150.0 :flexural-strength-max-mpa 260.0
                :thickness-deviation-actual-mm 0.05 :thickness-deviation-min-mm -0.10 :thickness-deviation-max-mm 0.10
                :eol-defect-unresolved? true
                :robotics-sim-verified? false :robotics-sim-record nil
                :glass-batch-shipped? false :glazing-certified? false
                :status :intake})
    "batch-5" (with-flexural-telemetry
               {:id "batch-5" :batch-name "佐藤ウィンドシールド Lot WS-09"
                :product-class :automotive-safety-glazing :jurisdiction "JPN"
                :panel-thickness-actual-mm 5.0 :flexural-test-pin-mass-kg 18.75
                :flexural-strength-min-mpa 150.0 :flexural-strength-max-mpa 260.0
                :thickness-deviation-actual-mm 0.05 :thickness-deviation-min-mm -0.10 :thickness-deviation-max-mm 0.10
                :eol-defect-unresolved? false
                :robotics-sim-verified? true :robotics-sim-record nil
                :glass-batch-shipped? false :glazing-certified? false
                :status :intake})
    "batch-6" (with-flexural-telemetry
               {:id "batch-6" :batch-name "Cover-Glass Lot CG-11 (6.1-inch-class handset)"
                :product-class :cover-glass :jurisdiction "COVER-GLASS"
                :panel-thickness-actual-mm 0.7 :flexural-test-pin-mass-kg 5.3
                :flexural-strength-min-mpa 450.0 :flexural-strength-max-mpa 900.0
                :thickness-deviation-actual-mm 0.02 :thickness-deviation-min-mm -0.05 :thickness-deviation-max-mm 0.05
                :eol-defect-unresolved? false
                :robotics-sim-verified? false :robotics-sim-record nil
                :glass-batch-shipped? false :glazing-certified? false
                :status :intake})}})

;; ----------------------------- shared commit logic -----------------------------

(defn- ship-batch!
  "Backend-agnostic `:glass-panel-batch/mark-shipped` -- looks up the
  batch via the protocol and drafts the glass-panel-batch-shipment
  record, and returns {:result .. :batch-patch ..} for the caller to
  persist."
  [s batch-id]
  (let [a (batch s batch-id)
        seq-n (next-shipment-sequence s (:jurisdiction a))
        result (registry/register-glass-panel-batch-shipment batch-id (:jurisdiction a) seq-n (:product-class a))]
    {:result result
     :batch-patch {:glass-batch-shipped? true
                    :shipment-number (get result "shipment_number")}}))

(defn- issue-glazing-certificate!
  "Backend-agnostic `:glass-panel-batch/mark-certified` -- looks up the
  batch via the protocol and drafts the Glazing Certificate record,
  and returns {:result .. :batch-patch ..} for the caller to persist."
  [s batch-id]
  (let [a (batch s batch-id)
        seq-n (next-evidence-sequence s (:jurisdiction a))
        result (registry/register-glazing-certificate batch-id (:jurisdiction a) seq-n)]
    {:result result
     :batch-patch {:glazing-certified? true
                    :evidence-number (get result "evidence_number")}}))

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (batch [_ id] (get-in @a [:batches id]))
  (all-batches [_] (sort-by :id (vals (:batches @a))))
  (eol-screen-of [_ id] (get-in @a [:eol-screens id]))
  (requirements-verification-of [_ batch-id] (get-in @a [:verifications batch-id]))
  (ledger [_] (:ledger @a))
  (shipment-history [_] (:shipments @a))
  (evidence-history [_] (:evidences @a))
  (next-shipment-sequence [_ jurisdiction] (get-in @a [:shipment-sequences jurisdiction] 0))
  (next-evidence-sequence [_ jurisdiction] (get-in @a [:evidence-sequences jurisdiction] 0))
  (batch-already-shipped? [_ batch-id] (boolean (get-in @a [:batches batch-id :glass-batch-shipped?])))
  (batch-already-certified? [_ batch-id] (boolean (get-in @a [:batches batch-id :glazing-certified?])))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :glass-panel-batch/upsert
      (swap! a update-in [:batches (:id value)] merge value)

      :verification/set
      (swap! a assoc-in [:verifications (first path)] payload)

      :eol-screen/set
      (swap! a assoc-in [:eol-screens (first path)] payload)

      :glass-panel-batch/mark-shipped
      (let [batch-id (first path)
            {:keys [result batch-patch]} (ship-batch! s batch-id)
            jurisdiction (:jurisdiction (batch s batch-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:shipment-sequences jurisdiction] (fnil inc 0))
                       (update-in [:batches batch-id] merge batch-patch)
                       (update :shipments registry/append result))))
        result)

      :glass-panel-batch/mark-certified
      (let [batch-id (first path)
            {:keys [result batch-patch]} (issue-glazing-certificate! s batch-id)
            jurisdiction (:jurisdiction (batch s batch-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:evidence-sequences jurisdiction] (fnil inc 0))
                       (update-in [:batches batch-id] merge batch-patch)
                       (update :evidences registry/append result))))
        result)
      nil)
    s)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-batches [s batches] (when (seq batches) (swap! a assoc :batches batches)) s))

(defn seed-db
  "A MemStore seeded with the demo batch set. The deterministic
  default."
  []
  (->MemStore (atom (assoc (demo-data)
                           :verifications {} :eol-screens {} :ledger [] :shipment-sequences {}
                           :shipments [] :evidence-sequences {} :evidences []))))

;; ----------------------------- DatomicStore (langchain.db) -----------------------------

(def ^:private schema
  "DataScript/Datomic-style schema: only constraint attrs are declared.
  Map/compound values (verification/eol-screen payloads, ledger facts,
  shipment/evidence records) are stored as EDN strings so `langchain.
  db` doesn't expand them into sub-entities -- the same convention
  every sibling actor's store uses."
  {:batch/id                          {:db/unique :db.unique/identity}
   :verification/batch-id             {:db/unique :db.unique/identity}
   :eol-screen/batch-id                {:db/unique :db.unique/identity}
   :ledger/seq                        {:db/unique :db.unique/identity}
   :shipment/seq                      {:db/unique :db.unique/identity}
   :evidence/seq                      {:db/unique :db.unique/identity}
   :shipment-sequence/jurisdiction    {:db/unique :db.unique/identity}
   :evidence-sequence/jurisdiction    {:db/unique :db.unique/identity}})

(defn- enc [v] (pr-str v))
(defn- dec* [s] (when s (edn/read-string s)))

(defn- batch->tx [{:keys [id batch-name product-class jurisdiction
                          panel-thickness-actual-mm flexural-test-pin-mass-kg
                          flexural-strength-min-mpa flexural-strength-max-mpa
                          sim-peak-flexural-force-n sim-peak-flexural-stress-mpa
                          thickness-deviation-actual-mm thickness-deviation-min-mm thickness-deviation-max-mm
                          eol-defect-unresolved? robotics-sim-verified? robotics-sim-record
                          glass-batch-shipped? glazing-certified?
                          status shipment-number evidence-number]}]
  (cond-> {:batch/id id}
    batch-name                                  (assoc :batch/batch-name batch-name)
    product-class                               (assoc :batch/product-class product-class)
    jurisdiction                                (assoc :batch/jurisdiction jurisdiction)
    panel-thickness-actual-mm                   (assoc :batch/panel-thickness-actual-mm panel-thickness-actual-mm)
    flexural-test-pin-mass-kg                   (assoc :batch/flexural-test-pin-mass-kg flexural-test-pin-mass-kg)
    flexural-strength-min-mpa                   (assoc :batch/flexural-strength-min-mpa flexural-strength-min-mpa)
    flexural-strength-max-mpa                   (assoc :batch/flexural-strength-max-mpa flexural-strength-max-mpa)
    sim-peak-flexural-force-n                   (assoc :batch/sim-peak-flexural-force-n sim-peak-flexural-force-n)
    sim-peak-flexural-stress-mpa                (assoc :batch/sim-peak-flexural-stress-mpa sim-peak-flexural-stress-mpa)
    thickness-deviation-actual-mm               (assoc :batch/thickness-deviation-actual-mm thickness-deviation-actual-mm)
    thickness-deviation-min-mm                  (assoc :batch/thickness-deviation-min-mm thickness-deviation-min-mm)
    thickness-deviation-max-mm                  (assoc :batch/thickness-deviation-max-mm thickness-deviation-max-mm)
    (some? eol-defect-unresolved?)              (assoc :batch/eol-defect-unresolved? eol-defect-unresolved?)
    (some? robotics-sim-verified?)               (assoc :batch/robotics-sim-verified? robotics-sim-verified?)
    (some? robotics-sim-record)                  (assoc :batch/robotics-sim-record (enc robotics-sim-record))
    (some? glass-batch-shipped?)                 (assoc :batch/glass-batch-shipped? glass-batch-shipped?)
    (some? glazing-certified?)                   (assoc :batch/glazing-certified? glazing-certified?)
    status                                      (assoc :batch/status status)
    shipment-number                             (assoc :batch/shipment-number shipment-number)
    evidence-number                             (assoc :batch/evidence-number evidence-number)))

(def ^:private batch-pull
  [:batch/id :batch/batch-name :batch/product-class :batch/jurisdiction
   :batch/panel-thickness-actual-mm :batch/flexural-test-pin-mass-kg
   :batch/flexural-strength-min-mpa :batch/flexural-strength-max-mpa
   :batch/sim-peak-flexural-force-n :batch/sim-peak-flexural-stress-mpa
   :batch/thickness-deviation-actual-mm :batch/thickness-deviation-min-mm :batch/thickness-deviation-max-mm
   :batch/eol-defect-unresolved? :batch/robotics-sim-verified? :batch/robotics-sim-record
   :batch/glass-batch-shipped? :batch/glazing-certified?
   :batch/status :batch/shipment-number :batch/evidence-number])

(defn- pull->batch [m]
  (when (:batch/id m)
    {:id (:batch/id m) :batch-name (:batch/batch-name m)
     :product-class (:batch/product-class m) :jurisdiction (:batch/jurisdiction m)
     :panel-thickness-actual-mm (:batch/panel-thickness-actual-mm m)
     :flexural-test-pin-mass-kg (:batch/flexural-test-pin-mass-kg m)
     :flexural-strength-min-mpa (:batch/flexural-strength-min-mpa m)
     :flexural-strength-max-mpa (:batch/flexural-strength-max-mpa m)
     :sim-peak-flexural-force-n (:batch/sim-peak-flexural-force-n m)
     :sim-peak-flexural-stress-mpa (:batch/sim-peak-flexural-stress-mpa m)
     :thickness-deviation-actual-mm (:batch/thickness-deviation-actual-mm m)
     :thickness-deviation-min-mm (:batch/thickness-deviation-min-mm m)
     :thickness-deviation-max-mm (:batch/thickness-deviation-max-mm m)
     :eol-defect-unresolved? (boolean (:batch/eol-defect-unresolved? m))
     :robotics-sim-verified? (boolean (:batch/robotics-sim-verified? m))
     :robotics-sim-record (dec* (:batch/robotics-sim-record m))
     :glass-batch-shipped? (boolean (:batch/glass-batch-shipped? m))
     :glazing-certified? (boolean (:batch/glazing-certified? m))
     :status (:batch/status m) :shipment-number (:batch/shipment-number m) :evidence-number (:batch/evidence-number m)}))

(defrecord DatomicStore [conn]
  Store
  (batch [_ id]
    (pull->batch (d/pull (d/db conn) batch-pull [:batch/id id])))
  (all-batches [_]
    (->> (d/q '[:find [?id ...] :where [?e :batch/id ?id]] (d/db conn))
         (map #(pull->batch (d/pull (d/db conn) batch-pull [:batch/id %])))
         (sort-by :id)))
  (eol-screen-of [_ id]
    (dec* (d/q '[:find ?p . :in $ ?aid
                :where [?k :eol-screen/batch-id ?aid] [?k :eol-screen/payload ?p]]
              (d/db conn) id)))
  (requirements-verification-of [_ batch-id]
    (dec* (d/q '[:find ?p . :in $ ?aid
                :where [?a :verification/batch-id ?aid] [?a :verification/payload ?p]]
              (d/db conn) batch-id)))
  (ledger [_]
    (->> (d/q '[:find ?s ?f :where [?e :ledger/seq ?s] [?e :ledger/fact ?f]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (shipment-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :shipment/seq ?s] [?e :shipment/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (evidence-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :evidence/seq ?s] [?e :evidence/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (next-shipment-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :shipment-sequence/jurisdiction ?j] [?e :shipment-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (next-evidence-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :evidence-sequence/jurisdiction ?j] [?e :evidence-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (batch-already-shipped? [s batch-id]
    (boolean (:glass-batch-shipped? (batch s batch-id))))
  (batch-already-certified? [s batch-id]
    (boolean (:glazing-certified? (batch s batch-id))))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :glass-panel-batch/upsert
      (d/transact! conn [(batch->tx value)])

      :verification/set
      (d/transact! conn [{:verification/batch-id (first path) :verification/payload (enc payload)}])

      :eol-screen/set
      (d/transact! conn [{:eol-screen/batch-id (first path) :eol-screen/payload (enc payload)}])

      :glass-panel-batch/mark-shipped
      (let [batch-id (first path)
            {:keys [result batch-patch]} (ship-batch! s batch-id)
            jurisdiction (:jurisdiction (batch s batch-id))
            next-n (inc (next-shipment-sequence s jurisdiction))]
        (d/transact! conn
                     [(batch->tx (assoc batch-patch :id batch-id))
                      {:shipment-sequence/jurisdiction jurisdiction :shipment-sequence/next next-n}
                      {:shipment/seq (count (shipment-history s)) :shipment/record (enc (get result "record"))}])
        result)

      :glass-panel-batch/mark-certified
      (let [batch-id (first path)
            {:keys [result batch-patch]} (issue-glazing-certificate! s batch-id)
            jurisdiction (:jurisdiction (batch s batch-id))
            next-n (inc (next-evidence-sequence s jurisdiction))]
        (d/transact! conn
                     [(batch->tx (assoc batch-patch :id batch-id))
                      {:evidence-sequence/jurisdiction jurisdiction :evidence-sequence/next next-n}
                      {:evidence/seq (count (evidence-history s)) :evidence/record (enc (get result "record"))}])
        result)
      nil)
    s)
  (append-ledger! [s fact]
    (d/transact! conn [{:ledger/seq (count (ledger s)) :ledger/fact (enc fact)}])
    fact)
  (with-batches [s batches]
    (when (seq batches) (d/transact! conn (mapv batch->tx (vals batches)))) s))

(defn datomic-store
  "A DatomicStore (langchain.db backend) seeded from `data`
  ({:batches ..}); empty when omitted."
  ([] (datomic-store {}))
  ([{:keys [batches]}]
   (let [s (->DatomicStore (d/create-conn schema))]
     (with-batches s batches))))

(defn datomic-seed-db
  "A DatomicStore seeded with the demo batch set -- the Datomic-
  backed analog of `seed-db`, used to prove protocol parity."
  []
  (datomic-store (demo-data)))
