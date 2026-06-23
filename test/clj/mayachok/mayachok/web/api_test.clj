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

(defn- fresh-uuid []
  (str (UUID/randomUUID)))

(defn- answers-low []
  [{:question 1 :answer 0} {:question 2 :answer 0} {:question 3 :answer 0}
   {:question 4 :answer 0} {:question 5 :answer 0} {:question 6 :answer 0}
   {:question 7 :answer 0} {:question 8 :answer 0} {:question 9 :answer 0}
   {:question 10 :answer 0}])

(defn- answers-high []
  [{:question 1 :answer 3} {:question 2 :answer 3} {:question 3 :answer 3}
   {:question 4 :answer 3} {:question 5 :answer 3} {:question 6 :answer 3}
   {:question 7 :answer 3} {:question 8 :answer 3} {:question 9 :answer 3}
   {:question 10 :answer 3}])

(defn- answers-self-harm []
  [{:question 1 :answer 0} {:question 2 :answer 0} {:question 3 :answer 0}
   {:question 4 :answer 0} {:question 5 :answer 0} {:question 6 :answer 0}
   {:question 7 :answer 0} {:question 8 :answer 0} {:question 9 :answer 0}
   {:question 10 :answer 2}])

(defn- answers-possible []
  [{:question 1 :answer 0} {:question 2 :answer 0} {:question 3 :answer 0}
   {:question 4 :answer 0} {:question 5 :answer 1} {:question 6 :answer 0}
   {:question 7 :answer 0} {:question 8 :answer 0} {:question 9 :answer 0}
   {:question 10 :answer 0}])

(defn- screening-insert [answers total-score q10-score risk-level
                         & {:keys [locale age-range time-since-birth
                                   first-child lat lng location-text]
                            :or   {locale "ru"}}]
  (create-screening!
   {:id               (fresh-uuid)
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

(defn- seed-data! []
  (clear-screenings!)
  ;; Screening 1: low-risk, Saint Petersburg, with survey data
  (screening-insert (answers-low) 9 0 :low-risk
                    :locale "ru"
                    :age-range "25-34"
                    :time-since-birth "0-6w"
                    :first-child "t"
                    :lat 59.93 :lng 30.32
                    :location-text "Saint Petersburg, Russia")

  ;; Screening 2: probable-depression, same location, no survey
  (screening-insert (answers-high) 21 3 :probable-depression
                    :locale "en"
                    :lat 59.93 :lng 30.32
                    :location-text "Saint Petersburg, Russia")

  ;; Screening 3: self-harm-risk, different location, no survey
  (screening-insert (answers-self-harm) 9 2 :self-harm-risk
                    :locale "de"
                    :lat 52.52 :lng 13.40
                    :location-text "Berlin, Germany")

  ;; Screening 4: possible-depression, no location
  (screening-insert (answers-possible) 10 0 :possible-depression
                    :locale "uk"
                    :age-range "35-44"
                    :first-child "f"))

;; -- tests -------------------------------------------------------------------

(deftest stats-returns-total-and-avg-score
  (testing "GET /api/stats returns total count and average score"
    (seed-data!)
    (let [handler (:handler/ring (system-state))
          response (GET handler "/api/stats" {} {})
          body     (parse-body response)]
      (is (= 200 (:status response)))
      (is (= 4 (:total body)))
      (is (number? (:avg_score body))))))

(deftest stats-risk-breakdown
  (testing "Risk breakdown counts match inserted data"
    (seed-data!)
    (let [handler (:handler/ring (system-state))
          response (GET handler "/api/stats" {} {})
          body     (parse-body response)
          risk     (:risk body)]
      (is (= 1 (:low-risk risk)))
      (is (= 1 (:probable-depression risk)))
      (is (= 1 (:self-harm-risk risk)))
      (is (= 1 (:possible-depression risk))))))

(deftest stats-survey-breakdown
  (testing "Survey breakdown reflects optional survey data"
    (seed-data!)
    (let [handler (:handler/ring (system-state))
          response (GET handler "/api/stats" {} {})
          body     (parse-body response)
          survey   (:survey body)]
      (is (= {:25-34 1 :35-44 1} (:age_range survey)))
      (is (= {:0-6w 1} (:time_since_birth survey)))
      (is (= {:t 1 :f 1} (:first_child survey))))))

(deftest stats-survey-nil-when-no-survey-data
  (testing "Survey fields are nil when no survey data exists"
    (clear-screenings!)
    (screening-insert (answers-low) 9 0 :low-risk)
    (let [handler (:handler/ring (system-state))
          response (GET handler "/api/stats" {} {})
          body     (parse-body response)
          survey   (:survey body)]
      (is (nil? (:age_range survey)))
      (is (nil? (:time_since_birth survey)))
      (is (nil? (:first_child survey))))))

(deftest stats-regions-array
  (testing "Regions array contains aggregated location data"
    (seed-data!)
    (let [handler (:handler/ring (system-state))
          response (GET handler "/api/stats" {} {})
          body     (parse-body response)
          regions  (:regions body)]
      (is (= 3 (count regions)))
      ;; Saint Petersburg should have 2 screenings
      (let [spb (first (filter #(= "Saint Petersburg, Russia" (:region %)) regions))]
        (is (some? spb))
        (is (= 2 (:total spb)))
        (is (= 1 (:probable_count spb)))
        (is (= 1 (:low_count spb))))
      ;; Berlin should have 1
      (let [ber (first (filter #(= "Berlin, Germany" (:region %)) regions))]
        (is (some? ber))
        (is (= 1 (:total ber)))
        (is (= 1 (:self_harm_count ber))))
      ;; unknown should have 1 (no location)
      (let [unk (first (filter #(= "unknown" (:region %)) regions))]
        (is (some? unk))
        (is (= 1 (:total unk)))))))

(deftest stats-region-contains-lat-lng
  (testing "Regions with coordinates include lat/lng"
    (seed-data!)
    (let [handler (:handler/ring (system-state))
          response (GET handler "/api/stats" {} {})
          body     (parse-body response)
          regions  (:regions body)
          spb      (first (filter #(= "Saint Petersburg, Russia" (:region %)) regions))]
      (is (number? (:lat spb)))
      (is (number? (:lng spb))))))

(deftest stats-region-unknown-has-no-coords
  (testing "Unknown region has nil lat/lng"
    (seed-data!)
    (let [handler (:handler/ring (system-state))
          response (GET handler "/api/stats" {} {})
          body     (parse-body response)
          regions  (:regions body)
          unk      (first (filter #(= "unknown" (:region %)) regions))]
      (is (nil? (:lat unk)))
      (is (nil? (:lng unk))))))

(deftest stats-empty-db
  (testing "Returns zeros when no screenings exist"
    (clear-screenings!)
    (let [handler (:handler/ring (system-state))
          response (GET handler "/api/stats" {} {})
          body     (parse-body response)]
      (is (= 200 (:status response)))
      (is (= 0 (:total body)))
      (is (= 0 (:low-risk (:risk body))))
      (is (= 0 (:probable-depression (:risk body))))
      (is (= 0 (:self-harm-risk (:risk body))))
      (is (= 0 (:possible-depression (:risk body))))
      (is (empty? (:regions body))))))

(deftest stats-no-answers-in-response
  (testing "Response does not contain individual screening answers"
    (seed-data!)
    (let [handler (:handler/ring (system-state))
          response (GET handler "/api/stats" {} {})
          body     (parse-body response)]
      ;; The response should NOT have a :screenings key with individual records
      (is (nil? (:screenings body))))))

(deftest health-endpoint-works
  (testing "GET /api/health returns 200"
    (let [handler (:handler/ring (system-state))
          response (GET handler "/api/health" {} {})]
      (is (= 200 (:status response))))))
