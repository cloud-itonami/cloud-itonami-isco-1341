(ns child-care-services.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [child-care-services.actor :as actor]
            [child-care-services.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-facility! st {:facility-id "facility-1" :name "Sunbeam Nursery"})
    st))

(deftest commits-a-clean-low-risk-request
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:facility-id "facility-1" :op :inspect :stake :low}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "facility-1"))))))

(deftest holds-on-unregistered-facility-without-committing
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:facility-id "no-such-facility" :op :inspect :stake :low}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :done (:status result)))
    (is (nil? (get-in result [:state :record])))
    (is (empty? (store/records-of st "no-such-facility")))
    (is (= :hold (:disposition (:state result))))))

(deftest interrupts-then-commits-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        ;; safety-incident clearance always escalates (governor invariant)
        request {:facility-id "facility-1" :op :clear-safety-incident :stake :high}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "facility-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (some? (get-in resumed [:state :record])))
      (is (= 1 (count (store/records-of st "facility-1")))))))
