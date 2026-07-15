# Operator Guide

## First Deployment
1. Register glazing quality engineers, float-lines/tempering-lines, glass-panel batches, personnel and robots.
2. Import historical batch / end-of-line / glazing-standard records.
3. Run read-only validation and robot flexural-bend-test mission dry-runs.
4. Configure glazing-standard evidence checklists and human sign-off paths.
5. Publish a dry-run audit export.

## Minimum Production Controls
- governor gate on every robot action before batch shipment
- human sign-off for `:high`/`:safety-critical` robot actions (e.g. batch shipment onward to a downstream consumer, Glazing Certificate issuance)
- audit export for every shipment, sign-off and disclosure
- backup manual process

## Certification
Certified operators must prove robot-safety integrity, evidence-backed
records and human review for safety-affecting actions.

## Operating states
intake : glazing-standard-rules-verify : end-of-line-quality-screen : flexural-strength-test-simulate : approve : ship-glass-panel-batch : issue-glazing-certificate : audit

## Audit export (social operation)

After a production session, export the append-only package for
market-regulator inspectors, downstream-consumer quality auditors or
internal compliance:

```clojure
(require '[glassworks.store :as store]
         '[glassworks.export :as export])
(export/audit-package store)        ; EDN maps
(export/package->csv-bundle store)  ; CSV files as string map
```

Drafts remain **unsigned** — signing and submission to a downstream
consumer or glazing-standard authority are the flat-glass
manufacturer's own acts (see README Actuation honesty).

Static UI sample: `docs/samples/operator-console.html`.
