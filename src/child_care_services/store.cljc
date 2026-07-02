(ns child-care-services.store
  "SSoT for the ISCO-08 1341 independent child-care-services-management
  sole-proprietor actor. Store is a protocol injected into the
  `child-care-services.actor` StateGraph — `MemStore` is the default,
  deterministic, zero-dep backend; a Datomic/kotoba-server-backed
  implementation can be swapped in without touching the actor or
  governor (itonami actor pattern, per ADR-2607011000 / CLAUDE.md
  Actors section).

  Domain:

    facility — a registered child-care facility (:facility-id, :name)
    record   — a committed operating record under a facility (staff
               plan, safety inspection, safety-incident clearance,
               staff-ratio exception approval) — written ONLY via
               commit-record!, never mutated in place
    ledger   — an append-only audit trail of every proposal/verdict/
               disposition, regardless of outcome (commit or hold)")

(defprotocol Store
  (facility [s facility-id])
  (records-of [s facility-id])
  (ledger [s])
  (register-facility! [s facility])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (facility [_ facility-id] (get-in @a [:facilities facility-id]))
  (records-of [_ facility-id] (filter #(= facility-id (:facility-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-facility! [s facility]
    (swap! a assoc-in [:facilities (:facility-id facility)] facility) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:facilities {} :records [] :ledger []} seed)))))
