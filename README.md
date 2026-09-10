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

## Reference implementation (`:maturity :implemented`)

Full itonami Actor pattern (per ADR-2607011000 / CLAUDE.md's Actors
section, alongside `cloud-itonami-isco-6130`, `-8160`, `-2166`, `-2641`,
`-2651`, `-2652`, `-2654`, `-1219`, `-1223` and `-1330`): a real
[`kotoba-lang/langgraph`](https://github.com/kotoba-lang/langgraph)
`StateGraph`, with the Advisor and Governor as distinct graph nodes and
human-in-the-loop interrupt/resume via checkpointing.

```text
:intake -> :advise -> :govern -> :decide -+-> :commit            (:ok? true)
                                           +-> :request-approval   (:escalate? true, interrupt-before)
                                           +-> :hold               (:hard? true)
```

- `src/child_care_services/store.kotoba` — `Store` protocol +
  `MemStore`: registered facilities, committed records, an append-only
  audit ledger.
- `src/child_care_services/advisor.kotoba` — `Advisor` protocol;
  `mock-advisor` (deterministic, default) proposes a services operation
  from a request; `llm-advisor` wraps a `langchain.model/ChatModel` —
  either way the advisor only ever produces a `:propose`-effect
  proposal, never a committed record, and LLM parse failures always
  yield `confidence 0.0` (forces escalation, never fabricated
  confidence).
- `src/child_care_services/governor.kotoba` —
  `ChildCareServicesGovernor/check`: a pure function, wired as its own
  `:govern` node. Hard invariants (unregistered facility, a proposal
  whose `:effect` isn't `:propose`) always route to `:hold`. Escalation
  invariants (`:clear-safety-incident`, `:approve-staff-ratio-exception`,
  or low advisor confidence) always route to `:request-approval` — an
  `interrupt-before` node that the graph checkpoints and only resumes on
  explicit human approval (`actor/approve!`), matching the README's
  robotics-premise statement that clearing a child-safety incident and
  approving a staff-ratio exception always require human sign-off.
- `src/child_care_services/actor.kotoba` — `build-graph`, `run-request!`,
  `approve!`: the `langgraph.graph/state-graph` wiring itself.

```bash
clojure -M:test
```

This is what backs this repo's `:maturity :implemented` entry in
[`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation).

## License

AGPL-3.0-or-later.
