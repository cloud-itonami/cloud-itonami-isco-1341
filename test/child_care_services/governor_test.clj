(ns child-care-services.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [child-care-services.store :as store]
            [child-care-services.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-facility! st {:facility-id "facility-1" :name "Sunbeam Nursery"})
    st))

(deftest ok-on-clean-inspect
  (let [st (fresh-store)
        proposal {:op :inspect :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:facility-id "facility-1"} {} proposal st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (not (:escalate? v)))))

(deftest hard-on-unregistered-facility
  (let [st (fresh-store)
        proposal {:op :inspect :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:facility-id "no-such-facility"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-facility (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        proposal {:op :inspect :effect :direct-write :confidence 0.9 :stake :low}
        v (governor/check {:facility-id "facility-1"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest escalates-on-safety-incident-clearance
  (let [st (fresh-store)
        proposal {:op :clear-safety-incident :effect :propose :confidence 0.9 :stake :high}
        v (governor/check {:facility-id "facility-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest escalates-on-staff-ratio-exception
  (let [st (fresh-store)
        proposal {:op :approve-staff-ratio-exception :effect :propose :confidence 0.9 :stake :high}
        v (governor/check {:facility-id "facility-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest escalates-on-low-confidence
  (let [st (fresh-store)
        proposal {:op :inspect :effect :propose :confidence 0.2 :stake :low}
        v (governor/check {:facility-id "facility-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest store-records-and-ledger-append-only
  (let [st (fresh-store)]
    (store/commit-record! st {:facility-id "facility-1" :op :staff-plan})
    (store/append-ledger! st {:disposition :commit})
    (is (= 1 (count (store/records-of st "facility-1"))))
    (is (= 1 (count (store/ledger st))))))
