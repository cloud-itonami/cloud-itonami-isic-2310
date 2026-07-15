# cloud-itonami-isic-2310

Open Business Blueprint for **ISIC Rev.5 2310**: manufacture of glass
and glass products -- glass-panel-batch intake, per-jurisdiction/
per-product-class glazing-standard rules verification, end-of-line
optical/edge-defect screening, robot flexural-strength-test (ASTM
C158) verification, batch-shipment dispatch and Glazing/Glass Test
Certificate finalization -- as an OSS business that any qualified
flat-glass manufacturer can fork, deploy, run, improve and sell, so a
plant keeps its own tempering and glazing-standard-conformance history
instead of renting a closed MES / quality SaaS.

Built on this workspace's
[`langgraph`](https://github.com/kotoba-lang/langgraph)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, Datomic/in-mem checkpoints) -- the same actor pattern as
every prior actor in this fleet -- here it is **Glass Advisor ⊣
Tempering Governor**.

## Scope note: the upstream glass stage feeding TWO downstream chains

This repository is scoped to **manufacturing flat glass and glass
products** (float-line/tempering-line intake, glazing-standard
evidence, end-of-line defect screening, flexural-strength-test
verification, batch-shipment and Glazing-Certificate issuance). It is
not a vehicle-assembly, device-assembly or glass-installation
vertical.

This actor's flat-glass manufacturer produces **two distinct product
classes**, and is the natural **upstream stage feeding both fleet
chains** built earlier this session:

- **automotive safety glazing** (windshields, tempered side/rear
  glass) -- ships onward to `cloud-itonami-isic-2910` (manufacture of
  motor vehicles, whose vehicle-assembly/type-approval lifecycle needs
  a finished, certified windshield/side-glass as an input) and
  `cloud-itonami-isic-2920` (motor-vehicle bodies/coachwork, whose
  `bodyshop.robotics` stamping-press body-shell forming needs tempered
  safety glazing for its own downstream glazing-installation step).
- **cover glass** (chemically-strengthened glass for smartphones/
  electronics, e.g. the Gorilla-Glass-style category) -- ships onward
  to `cloud-itonami-isic-2630` (manufacture of communication
  equipment, whose `commsdevice.robotics` display-module optical-
  bonding [OCA lamination] press-run test literally assumes a
  cover-glass panel already exists as an input).

Both product classes are, at bottom, the SAME manufacturing act --
producing a flat-glass panel to a tempering/chemical-strengthening
spec and a flexural-strength test result -- just different
tempering/composition specs and different glazing-standard citations
(see `glassworks.facts`). This actor is the missing upstream stage
`cloud-itonami-isic-2630` and `cloud-itonami-isic-2910`/`-2920` were
each written assuming already existed. Distinct from:

- `cloud-itonami-isic-2910` -- motor-vehicle **assembly** (downstream consumer of automotive glazing)
- `cloud-itonami-isic-2920` -- motor-vehicle body/coachwork **manufacturing** (downstream consumer of automotive glazing)
- `cloud-itonami-isic-2630` -- communication-equipment **manufacturing** (downstream consumer of cover glass)

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here robots (float-line/
tempering-line handling, end-of-line optical scan, flexural-bend-test
loading) operate under an actor that proposes actions and an
independent **Tempering Governor** that gates them. The governor
never issues a Glazing Certificate itself; `:high`/`:safety-critical`
actions (`:actuation/ship-glass-panel-batch`,
`:actuation/issue-glazing-certificate`) require human sign-off.

**Robot process simulation is a REAL, native time-stepped physics
simulation, not a flag** (ADR-2607151600, ADR-2607152000,
ADR-2607160600): `glassworks.robotics` walks every glass-panel batch
through a robot-executed ASTM C158 three-point flexural (bend)
strength test -- a real `kotoba-lang/physics-2d` rigid-body
simulation of a loading pin closing onto a supported glass-panel
specimen -- before `:actuation/ship-glass-panel-batch` is proposable.
`:sim-peak-flexural-force-n` is read directly off the ACTUAL simulated
collision trajectory (F = m·a), and `:sim-peak-flexural-stress-mpa`
converts that force into a genuine textbook flexural-stress reading
via the standard rectangular-beam center-loading formula
`sigma = 3·F·L / (2·w·h^2)`. The Tempering Governor independently
re-derives whether the batch's own recorded flexural-stress reading
falls out of its own recorded tolerance bounds, never trusting the
mission's self-reported verdict alone. See `glassworks.robotics`'s
docstring for the full honesty disclosure (which numbers are cited
published anchors vs. reasoned engineering estimates).

## Core contract

```text
glass-panel-batch intake + glazing-standard rules verify + end-of-line quality screen
  -> Glass Advisor proposal
  -> Tempering Governor (HARD holds un-overridable)
  -> phase gate (actuation always escalates)
  -> human approval for high stakes
  -> append-only ledger + draft records
```

## Actuation honesty

Shipping a glass-panel batch onward to a downstream consumer and
issuing a Glazing/Glass Test Certificate produce **unsigned draft
records and ledger facts only**. This actor does not talk to real
plant control systems or downstream-consumer receiving systems.
Signature and physical dispatch are the flat-glass manufacturer's own
acts.

## Ops

| Op | Effect |
|---|---|
| `:glass-panel-batch/intake` | normalize glass-panel-batch directory patch (phase 3 may auto-commit when clean) |
| `:glazing-standard-rules/verify` | per-jurisdiction/per-product-class glazing-standard evidence checklist (always human) |
| `:end-of-line-quality/screen` | end-of-line optical-clarity/edge-defect/inclusion defect screen (HARD hold if unresolved) |
| `:robotics/simulate-flexural-strength-test` | REAL `physics-2d`-simulated ASTM C158 flexural-bend-test mission (always human; required on file before shipment) |
| `:actuation/ship-glass-panel-batch` | draft glass-panel-batch-shipment record (always human; HARD hold if robotics-sim missing/out-of-tolerance or thickness deviation out of range) |
| `:actuation/issue-glazing-certificate` | draft Glazing/Glass Test Certificate record (always human) |

## Social / regulatory hand-off

```clojure
(require '[glassworks.store :as store]
         '[glassworks.export :as export])

(def db (store/seed-db))
(export/audit-package db)           ;; EDN maps for regulator/downstream-consumer hand-off
(export/package->csv-bundle db)     ;; CSV bundle (batches/ledger/shipments/glazing-certificates)
```

Operator console (static sample): `docs/samples/operator-console.html`.

## Develop

```bash
clojure -M:dev:test
clojure -M:lint
clojure -M:dev:run
```

## License

AGPL-3.0-or-later -- see `LICENSE`.

## Operator console (Pages)

After enabling GitHub Pages (Settings -> Pages -> GitHub Actions), the
static console is at:

https://cloud-itonami.github.io/cloud-itonami-isic-2310/

Local: open `docs/samples/operator-console.html`.

## Export audit package (CLI)

```bash
clojure -M:dev:export
# or: clojure -M:dev:export /tmp/audit-2310
```

Writes CSV files under `out/audit-package/` (or the given directory).
