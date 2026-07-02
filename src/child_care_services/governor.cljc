(ns child-care-services.governor
  "ChildCareServicesGovernor — the independent safety/traceability
  layer for the ISCO-08 1341 independent child-care-services-management
  actor. Wired as its own `:govern` node in
  `child-care-services.actor`'s StateGraph, downstream of `:advise` —
  the Advisor has no notion of facility provenance or child-safety
  risk, so this MUST be a separate system able to reject a proposal
  (itonami actor pattern, per ADR-2607011000 / CLAUDE.md Actors
  section).

  `check` is a pure function of (request, context, proposal, store) ->
  verdict; it never mutates the store. The StateGraph's `:decide` node
  routes on the verdict:
    :hard? true                → :hold  (irreversible, no write)
    :escalate? true            → :request-approval (interrupt-before)
    otherwise                  → :commit

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. facility provenance  — the request's facility must be registered.
    2. no-actuation           — proposal :effect must be :propose.
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off, per the
  README robotics-premise: clearing a child-safety incident and
  approving a staff-ratio exception always require human sign-off):
    3. :op :clear-safety-incident.
    4. :op :approve-staff-ratio-exception.
    5. low confidence (< `confidence-floor`)."
  (:require [child-care-services.store :as store]))

(def confidence-floor 0.6)
(def ^:private escalating-ops #{:clear-safety-incident :approve-staff-ratio-exception})

(defn- hard-violations [{:keys [proposal]} facility-record]
  (cond-> []
    (nil? facility-record)
    (conj {:rule :no-facility :detail "未登録 facility"})

    (not= :propose (:effect proposal))
    (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"})))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `child-care-services.store/Store`. Returns
  `{:ok? bool :violations [...] :confidence n :hard? bool :escalate? bool}`."
  [request context proposal store]
  (let [facility-record (store/facility store (:facility-id request))
        hard (hard-violations {:proposal proposal} facility-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        risky-op? (contains? escalating-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not risky-op?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? risky-op?))}))
