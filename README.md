# cloud-itonami-isco-1341

Open Occupation Blueprint for **ISCO-08 1341**: Child Care Services Managers.

This repository designs a forkable OSS business for an independent child care services manager: a facility-walkthrough robot performs safety-checklist inspection under a governor-gated actor, so the practice keeps its own staffing and safety records instead of renting a closed childcare-management SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a facility-walkthrough robot performs safety-checklist inspection and supply-restocking tasks under an actor that proposes
actions and an independent **Child Care Services Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
clearing a child-safety incident, or approving a staff-ratio exception) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
facility plan + staffing ratio + safety checklist
        |
        v
Services Advisor -> Child Care Services Governor -> staff-plan/inspect, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `1341`). Required capabilities:

- :robotics
- :identity
- :forms
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
