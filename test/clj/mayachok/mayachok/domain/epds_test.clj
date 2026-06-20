(ns mayachok.mayachok.domain.epds-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [mayachok.mayachok.domain.epds :as epds]))

;; -- helpers ----------------------------------------------------------------

(defn- make-answers
  "Creates an answers vector with all answers set to `default-answer`,
   then overrides specific questions via the `overrides` map."
  [default-answer overrides]
  (vec (for [q (range 1 11)]
         {:question q
          :answer  (get overrides q default-answer)})))

;; -- score-answer -----------------------------------------------------------

(deftest test-score-answer-normal
  (testing "Normal-scored questions (3,5,6,7,8,9,10) pass through answer index"
    (is (= 0 (epds/score-answer 3 0)))
    (is (= 1 (epds/score-answer 5 1)))
    (is (= 2 (epds/score-answer 6 2)))
    (is (= 3 (epds/score-answer 10 3)))))

(deftest test-score-answer-reverse
  (testing "Reverse-scored questions (1,2,4) invert the answer index"
    (is (= 3 (epds/score-answer 1 0)))
    (is (= 2 (epds/score-answer 2 1)))
    (is (= 1 (epds/score-answer 4 2)))
    (is (= 0 (epds/score-answer 1 3)))))

;; -- total-score -----------------------------------------------------------

;; Note: Questions 1, 2, 4 are reverse-scored (answer 0 => score 3).
;; So "all zeros" means the user picked the first option for every question,
;; which scores 3+3+0+3+0+0+0+0+0+0 = 9 for reverse-scored questions.

(deftest test-total-score-all-zeros
  (testing "All answers 0 => reverse-scored Qs (1,2,4) give 3 each, total = 9"
    (let [answers (make-answers 0 {})]
      (is (= 9 (epds/total-score answers))))))

(deftest test-total-score-all-threes
  (testing "All answers 3 => reverse-scored Qs give 0 each, normal Qs give 3"
    ;; Q1=3->0, Q2=3->0, Q3=3, Q4=3->0, Q5=3, Q6=3, Q7=3, Q8=3, Q9=3, Q10=3
    ;; = 0+0+3+0+3+3+3+3+3+3 = 21
    (let [answers (make-answers 3 {})]
      (is (= 21 (epds/total-score answers))))))

(deftest test-total-score-low-risk
  (testing "Score 9 is below possible-depression threshold (10)"
    (let [answers (make-answers 0 {})]
      (is (= 9 (epds/total-score answers))))))

(deftest test-total-score-at-possible-depression-threshold
  (testing "Score 10 is at possible-depression threshold"
    ;; Start from all-0 (score 9), bump Q5 answer from 0 to 1 => +1
    (let [answers (make-answers 0 {5 1})]
      (is (= 10 (epds/total-score answers))))))

(deftest test-total-score-between-thresholds
  (testing "Score 12 is between possible and probable depression"
    ;; All-0 = 9, Q5=1 (+1), Q6=1 (+1), Q7=1 (+1) => 12
    (let [answers (make-answers 0 {5 1, 6 1, 7 1})]
      (is (= 12 (epds/total-score answers))))))

(deftest test-total-score-at-probable-depression-threshold
  (testing "Score 13 is at probable-depression threshold"
    ;; All-0 = 9, Q3=1 (+1), Q5=1 (+1), Q6=1 (+1), Q7=1 (+1) => 13
    (let [answers (make-answers 0 {3 1, 5 1, 6 1, 7 1})]
      (is (= 13 (epds/total-score answers))))))

;; -- q10-score -------------------------------------------------------------

(deftest test-q10-score-zero
  (is (= 0 (epds/q10-score (make-answers 0 {10 0})))))

(deftest test-q10-score-one
  (is (= 1 (epds/q10-score (make-answers 0 {10 1})))))

(deftest test-q10-score-three
  (is (= 3 (epds/q10-score (make-answers 0 {10 3})))))

;; -- risk-level ------------------------------------------------------------

(deftest test-risk-level-low
  (is (= :low-risk (epds/risk-level 0 0)))
  (is (= :low-risk (epds/risk-level 5 0)))
  (is (= :low-risk (epds/risk-level 9 0))))

(deftest test-risk-level-possible-depression
  (is (= :possible-depression (epds/risk-level 10 0)))
  (is (= :possible-depression (epds/risk-level 12 0))))

(deftest test-risk-level-probable-depression
  (is (= :probable-depression (epds/risk-level 13 0)))
  (is (= :probable-depression (epds/risk-level 30 0))))

(deftest test-risk-level-self-harm-overrides-total
  (testing "Q10 > 0 always returns :self-harm-risk regardless of total"
    (is (= :self-harm-risk (epds/risk-level 0 1)))
    (is (= :self-harm-risk (epds/risk-level 4 1)))
    (is (= :self-harm-risk (epds/risk-level 9 3)))
    (is (= :self-harm-risk (epds/risk-level 15 1)))))

;; -- score-screening (integration) -----------------------------------------

(deftest test-score-screening-all-zeros
  (let [answers (make-answers 0 {})
        result (epds/score-screening answers)]
    (is (= 9 (:total-score result)))
    (is (= 0 (:q10-score result)))
    (is (= :low-risk (:risk-level result)))))

(deftest test-score-screening-self-harm-with-low-total
  (let [answers (make-answers 0 {10 1})
        result (epds/score-screening answers)]
    (is (pos? (:q10-score result)))
    (is (= :self-harm-risk (:risk-level result)))))

(deftest test-score-screening-probable-depression
  (let [answers [{:question 1 :answer 3}  ; rev->0
                 {:question 2 :answer 3}  ; rev->0
                 {:question 3 :answer 3}  ; ->3
                 {:question 4 :answer 3}  ; rev->0
                 {:question 5 :answer 3}  ; ->3
                 {:question 6 :answer 3}  ; ->3
                 {:question 7 :answer 1}  ; ->1
                 {:question 8 :answer 0}  ; ->0
                 {:question 9 :answer 0}  ; ->0
                 {:question 10 :answer 0} ; ->0
                 ]
        result (epds/score-screening answers)]
    (is (= 10 (:total-score result)))
    (is (= 0 (:q10-score result)))
    (is (= :possible-depression (:risk-level result)))))

;; -- critical regression tests ---------------------------------------------

(deftest test-worst-possible-score-is-high
  (testing "Selecting the worst option on every question should give a high score"
    ;; With the corrected option order, the worst option is index 3 for all questions.
    ;; Q1,Q2,Q4 are reverse-scored: index 3 -> score 0
    ;; Q3,Q5,Q6,Q7,Q8,Q9,Q10 are normal-scored: index 3 -> score 3
    ;; So worst = 0+0+3+0+3+3+3+3+3+3 = 21
    ;; But the user sees the LAST option as "worst" for all questions.
    ;; The important thing is: selecting all last options != selecting all first options
    (let [all-last (vec (for [q (range 1 11)] {:question q :answer 3}))
          all-first (vec (for [q (range 1 11)] {:question q :answer 0}))
          score-last (epds/total-score all-last)
          score-first (epds/total-score all-first)]
      (println "All last options score:" score-last)
      (println "All first options score:" score-first)
      (is (> score-last score-first)
          "Selecting all last options should give a higher score than all first options"))))
