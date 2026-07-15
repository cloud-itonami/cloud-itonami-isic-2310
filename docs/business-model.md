# Business Model: Manufacture of Glass and Glass Products

## Classification
- Repository: `cloud-itonami-isic-2310`
- ISIC Rev.5: `2310` — manufacture of glass and glass products — flat-glass batch intake, glazing-standard rules verification, end-of-line quality screening, flexural-strength-test verification and Glazing Certificate issuance
- Social impact: vehicle-safety, product-safety, supply-resilience, industrial-jobs

## Customer
- independent flat-glass manufacturers producing automotive safety glazing and/or cover glass
- contract tempering/chemical-strengthening lines serving multiple downstream OEMs
- plant operators needing verifiable batch and end-of-line history for shipped glass-panel lots
- downstream consumers (`cloud-itonami-isic-2910`/`-2920` motor-vehicle plants, `cloud-itonami-isic-2630` communication-equipment plants) needing verifiable glazing-standard-conformance evidence on incoming glass
- market regulators needing verifiable glazing-standard type-approval and conformity evidence
- programs that cannot accept closed, unauditable manufacturing-execution platforms

## Offer
- glazing-standard rules and jurisdiction/product-class-scope version management
- robotics-assisted flexural-strength (ASTM C158) bend-test verification records, backed by a REAL time-stepped physics simulation
- glass-panel thickness-deviation and end-of-line chain-of-custody history
- Glazing/Glass Test Certificate drafts and disclosure records
- role-based access and immutable audit ledger
- CSV/EDN audit package export for inspectors and downstream consumers

## Revenue
- self-host setup fee
- managed hosting subscription per plant / float-line
- support retainer with SLA
- flexural-test-cell/tempering-line robot integration and maintenance

## Trust Controls
- out-of-spec glass-panel batches are blocked; a Glazing Certificate is mandatory for release paths; batch history is immutable
- a robot action the governor refuses is never dispatched to hardware
- every shipment, hold, approval and disclosure path is auditable
- sensitive plant, formulation and production data stays outside Git
- a fabricated glazing-standard-rules citation, incomplete evidence, an
  out-of-spec thickness deviation, a genuinely UNDER-TEMPERED flexural-
  strength reading, or an unresolved end-of-line defect -- each forces
  a hold, not an override
- Glazing Certificate issuance is logged and escalated, and cannot be
  finalized twice for the same batch
