(ns mayachok.mayachok.web.api-test
  (:require
    [clojure.data.json :as json]
    [clojure.test :refer [deftest testing is use-fixtures]]
    [mayachok.mayachok.test-utils :refer [system-state system-fixture GET]])
  (:import [java.util UUID]))

(use-fixtures :once (system-fixture))

;; -- helpers ----------------------------------------------------------------

(defn- query-fn []
  (:db.sql/query-fn (system-state)))

(defn- db-call [query-name params]
  ((query-fn) query-name params))

(defn- create-screening! [params]
  (db-call :create-screening! params))

(defn- clear-screenings! []
  (db-call :clear-screenings! {}))

(defn- iso-now []
  (str (java.time.Instant/now)))

(defn- id-one []
  [{:question 1 :answer 0} {:question 2 :answer 0} {:question 3 :answer 0}
   {:question 4 :answer 0} {:question 5 :answer 0} {:question 6 :answer 0}
   {:question 7 :answer 0} {:question 8 :answer 0} {:question 9 :answer 0}
   {:question 10 :answer 0}])

(defn- id-two []
  [{:question 1 :answer 3} {:question 2 :answer 3} {:question 3 :answer 3}
   {:question 4 :answer 3} {:question 5 :answer 3} {:question 6 :answer 3}
   {:question 7 :answer 3} {:question 8 :answer 3} {:question 9 :answer 3}
   {:question 10 :answer 3}])

(defn- id-three []
  [{:question 1 :answer 0} {:question 2 :answer 0} {:question 3 :answer 0}
   {:question 4 :answer 0} {:question 5 :answer 0} {:question 6 :answer 0}
   {:question 7 :answer 0} {:question 8 :answer 0} {:question 9 :answer 0}
   {:question 10 :answer 2}])

(defn- screening-insert [id answers total-score q10-score risk-level
                         & {:keys [locale age-range time-since-birth
                                   first-child lat lng location-text]
                            :or   {locale "ru"}}]
  (create-screening!
    {:id               id
     :created_at       (iso-now)
     :locale           locale
     :answers          (json/write-str answers)
     :total_score      total-score
     :q10_score        q10-score
     :risk_level       (name risk-level)
     :age_range        age-range
     :time_since_birth time-since-birth
     :first_child      first-child
     :lat              lat
     :lng              lng
     :location_text    location-text}))

(defn- parse-body [response]
  (json/read-str (:body response) :key-fn keyword))

;; -- tests -------------------------------------------------------------------

(defn- fresh-uuid []
  (str (java.util.UUID/randomUUID)))

(defn- seed-mock-data! []
  (clear-screenings!)
  ;; Screening 1: low-risk, Saint Petersburg, with survey data
  (screening-insert (fresh-uuid)
                    (id-one) 9 0 :low-risk
                    :locale "ru"
                    :age-range "25-34"
                    :time-since-birth "0-6w"
                    :first-child "t"
                    :lat 59.93 :lng 30.32
                    :location-text "Saint Petersburg, Russia")

  ;; Screening 2: probable-depression, same location, no survey
  (screening-insert (fresh-uuid)
                    (id-two) 21 3 :probable-depression
                    :locale "en"
                    :lat 59.93 :lng 30.32
                    :location-text "Saint Petersburg, Russia")

  ;; Screening 3: self-harm-risk, different location, no survey
  (screening-insert (fresh-uuid)
                    (id-three) 9 2 :self-harm-risk
                    :locale "de"
                    :lat 52.52 :lng 13.40
                    :location-text "Berlin, Germany")

  ;; Screening 4: possible-depression, no location
  (screening-insert (fresh-uuid)
                    ;; Q1 rev 0->3, Q2 rev 0->3, Q3 norm 0->0, Q4 rev 0->3
                    ;; Q5 1, others 0 => 3+3+0+3+1 = 10
                    [{:question 1 :answer 0} {:question 2 :answer 0}
                     {:question 3 :answer 0} {:question 4 :answer 0}
                     {:question 5 :answer 1} {:question 6 :answer 0}
                     {:question 7 :answer 0} {:question 8 :answer 0}
                     {:question 9 :answer 0} {:question 10 :answer 0}]
                    10 0 :possible-depression
                    :locale "uk"
                    :age-range "35-44"
                    :first-child "f"))

(deftest stats-returns-all-screenings
  (testing "GET /api/stats returns total count and all screenings"
    (seed-mock-data!)
    (let [handler (:handler/ring (system-state))
          response (GET handler "/api/stats" {} {})
          body     (parse-body response)]
      (is (= 200 (:status response)))
      (is (= 4 (:total body)))
      (is (= 4 (count (:screenings body)))))))

(deftest stats-risk-breakdown
  (testing "Aggregates risk levels correctly"
    (let [handler (:handler/ring (system-state))
          response (GET handler "/api/stats" {} {})
          body     (parse-body response)
          risk     (:risk body)]
      (is (= 1 (:low-risk risk)))
      (is (= 1 (:probable-depression risk)))
      (is (= 1 (:self-harm-risk risk)))
      (is (= 1 (:possible-depression risk))))))

(deftest stats-region-breakdown
  (testing "Groups screenings by location_text"
    (let [handler (:handler/ring (system-state))
          response (GET handler "/api/stats" {} {})
          body     (parse-body response)
          regions  (:regions body)
          get-region (fn [region-name] (some (fn [[k v]] (when (= (name k) region-name) v)) regions))]
      (is (= 2 (get-region "Saint Petersburg, Russia")))
      (is (= 1 (get-region "Berlin, Germany")))
      (is (= 1 (get-region "unknown"))))))

(deftest stats-screenings-array-contains-expected-fields
  (testing "Each screening has the right fields and no answers JSON"
    (let [handler (:handler/ring (system-state))
          response (GET handler "/api/stats" {} {})
          body     (parse-body response)
          s        (first (:screenings body))]
      (is (some? (:id s)))
      (is (some? (:created_at s)))
      (is (some? (:locale s)))
      (is (some? (:total_score s)))
      (is (some? (:q10_score s)))
      (is (some? (:risk_level s)))
      ;; answers JSON should NOT be in the stats output
      (is (nil? (:answers s))))))

(deftest stats-screenings-sorted-by-created-at-desc
  (testing "Screenings are ordered newest first"
    (let [handler (:handler/ring (system-state))
          response (GET handler "/api/stats" {} {})
          body     (parse-body response)
          times    (map :created_at (:screenings body))]
      ;; Each created_at should be >= the one after it (descending order)
      (is (every? (fn [[a b]] (>= (compare a b) 0))
                  (partition 2 1 times))))))

(deftest stats-filter-by-risk
  (testing "?risk=self-harm-risk returns only that risk level"
    (let [handler (:handler/ring (system-state))
          response (GET handler "/api/stats" {:risk "self-harm-risk"} {})
          body     (parse-body response)]
      (is (= 1 (:total body)))
      (is (= 1 (count (:screenings body))))
      (is (= "self-harm-risk" (:risk_level (first (:screenings body))))))))

(deftest stats-filter-by-limit
  (testing "?limit=2 caps the screenings array"
    (let [handler (:handler/ring (system-state))
          response (GET handler "/api/stats" {:limit "2"} {})
          body     (parse-body response)]
      (is (= 4 (:total body)))
      (is (= 2 (count (:screenings body)))))))
