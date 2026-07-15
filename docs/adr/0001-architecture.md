# ADR-0001: Glass Advisor ⊣ Tempering Governor architecture

- Status: Accepted (2026-07-16)
- Repository: `cloud-itonami-isic-2310` (ISIC Rev.5 `2310`)

## Context

Flat-glass manufacturing (float-line/tempering, end-of-line optical/
edge-defect inspection, glazing-standard verification, flexural-
strength-test certification, batch-shipment dispatch) needs the same
governed-actor pattern as the rest of the cloud-itonami fleet: an
untrusted advisor proposes; an independent governor may HOLD;
high-stakes actuation never auto-commits.

This vertical is the missing UPSTREAM stage two prior fleet
verticals already assumed as an input: `cloud-itonami-isic-2630`
(communication-equipment manufacturing)'s display-module optical-
bonding press test assumes a cover-glass panel already exists, and
`cloud-itonami-isic-2920` (motor-vehicle bodies/coachwork)'s downstream
sibling `cloud-itonami-isic-2910` (motor-vehicle assembly) needs
windshield/side-glass safety glazing. Automotive safety glazing and
smartphone cover glass are both fundamentally glass-manufacturing
outputs of the SAME manufacturing act (a float-line/tempering-line
producing a flat-glass panel to spec), differing only in tempering/
chemical-strengthening process and glazing-standard citation.

This vertical is built DIRECTLY to ADR-2607151600/ADR-2607152000's
real-physics-simulation standard from day one (a NEW actor, not a
retrofit), mirroring how `cloud-itonami-isic-2920`/`-2930`/`-2394`
deliver it natively.

## Decision

1. Namespaces live under `glassworks.*` with the standard facts /
   registry / store / robotics / governor / phase / advisor /
   operation / sim / export shape.
2. Entity is a **glass-panel batch** (a manufactured lot of glass
   panels of one spec), not a vehicle, device unit or body shell.
3. Dual actuation on the same entity:
   - `:actuation/ship-glass-panel-batch` (batch-shipment dispatch draft to a downstream consumer)
   - `:actuation/issue-glazing-certificate` (Glazing/Glass Test Certificate draft)
4. Double-actuation guards use dedicated booleans
   (`:glass-batch-shipped?`, `:glazing-certified?`), never a status
   lifecycle (ADR-2607071320 / 6492 lesson).
5. `panel-thickness-deviation-out-of-range?` continues the fleet
   two-sided range check family, applied here to a batch's own
   measured thickness deviation against its own recorded spec bounds
   -- a check SEPARATE from the robotics-simulated flexural-strength
   reading.
6. `glassworks.robotics` runs a REAL time-stepped `kotoba-lang/
   physics-2d` rigid-body simulation of an ASTM C158 three-point
   flexural (bend) strength test: a loading-pin `Body2D` closes on a
   static (mass 0) glass-panel-specimen `Body2D`; `:sim-peak-
   flexural-force-n` is derived from the actual simulated collision
   trajectory (F = m·a); `:sim-peak-flexural-stress-mpa` applies the
   genuine textbook rectangular-beam center-loading formula
   `sigma = 3·F·L/(2·w·h^2)`. The Tempering Governor independently
   rechecks this persisted reading against the batch's own recorded
   acceptance band, never trusting the mission's self-reported
   verdict -- the SAME discipline `bodyshop.robotics`/`commsdevice.
   robotics` established.
7. Flexural-strength acceptance bands are seeded per product class
   with EXPLICIT confidence disclosure: annealed float glass
   (~35-55 MPa) is a confident general anchor; automotive-tempered
   (~150-260 MPa) and chemically-strengthened cover glass
   (~450-900 MPa) are reasoned engineering estimates, disclosed as
   such -- never presented as verbatim single citations.
8. End-of-line defect unresolved is evaluated unconditionally so
   `:end-of-line-quality/screen` itself can HARD-hold (the same
   discipline every prior sibling actor's ADR-0001 already recorded).
9. Glazing-standard catalog seeds USA (NHTSA/ANSI-SAE Z26.1, FMVSS
   205, alongside ASTM C1036 as the flat-glass baseline spec) / JPN
   (MLIT/JISC, JIS R 3211) / UNECE (WP.29, UNECE Regulation No. 43,
   the multi-market EU/Japan/1958-Agreement basis) for automotive
   safety glazing, and a separate COVER-GLASS entry (ASTM
   International, ASTM C158) for cover/chemically-strengthened glass
   -- missing jurisdictions/product classes are uncovered, never
   fabricated.
10. `glassworks.registry/register-glass-panel-batch-shipment` records
    the REAL two-member downstream-consumer mapping honestly:
    automotive-safety-glazing batches -> `cloud-itonami-isic-2910`/
    `cloud-itonami-isic-2920`; cover-glass batches ->
    `cloud-itonami-isic-2630`.

## Consequences

(+) Flat-glass manufacturing gains a forkable OSS operating stack
with auditable governor holds, closing the upstream gap two sibling
verticals already assumed.
(+) Reuses langgraph + store dual-backend parity + the fleet's
real-physics-simulation pattern without inventing a new architecture.
(-) No physical furnace/tempering-line digital-twin tick in this
repo (follow-up domain data is out of scope here).
(-) Glazing-standard-authority coverage is a starting catalog, not
exhaustive (e.g. no Germany/EU-specific national statute beyond the
UNECE R43 multi-market basis).
(-) `physics-2d`'s AABB collider models a three-point/center-loading
bend test only; ASTM C158's four-point loading geometry is not
modeled (disclosed in `glassworks.robotics`'s docstring).

## Related

- Superproject fleet ADR for this promotion: ADR-2607160600
  (`90-docs/adr/2607160600-cloud-itonami-isic-2310-glassworks.md`)
- Real-physics-simulation pattern: ADR-2607151600, ADR-2607152000
- Sibling architecture: `cloud-itonami-isic-2910` docs/adr/0001,
  `cloud-itonami-isic-2920` docs/adr/0001,
  `cloud-itonami-isic-2630` docs/adr/0001
