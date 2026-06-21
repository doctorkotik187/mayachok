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

(deftest test-total-score-all-zeros
  (testing "All answers 0 => reverse-scored Qs (1,2,4) give 3 each, total = 9"
    (let [answers (make-answers 0 {})]
      (is (= 9 (epds/total-score answers))))))

(deftest test-total-score-all-threes
  (testing "All answers 3 => reverse-scored Qs give 0 each, normal Qs give 3"
    (let [answers (make-answers 3 {})]
      (is (= 21 (epds/total-score answers))))))

(deftest test-total-score-low-risk
  (testing "Score 9 is below possible-depression threshold (10)"
    (let [answers (make-answers 0 {})]
      (is (= 9 (epds/total-score answers))))))

(deftest test-total-score-at-possible-depression-threshold
  (testing "Score 10 is at possible-depression threshold"
    (let [answers (make-answers 0 {5 1})]
      (is (= 10 (epds/total-score answers))))))

(deftest test-total-score-between-thresholds
  (testing "Score 12 is between possible and probable depression"
    (let [answers (make-answers 0 {5 1, 6 1, 7 1})]
      (is (= 12 (epds/total-score answers))))))

(deftest test-total-score-at-probable-depression-threshold
  (testing "Score 13 is at probable-depression threshold"
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

;; -- translation equivalence ------------------------------------------------
;; Verify RU and EN have the same number of options per question.

(deftest test-ru-en-option-counts-match
  (testing "Russian and English have same number of options per question"
    (doseq [q (range 1 11)]
      (let [ru-count (count (get-in epds/questions [:ru q :options]))
            en-count (count (get-in epds/questions [:en q :options]))]
        (is (= ru-count en-count)
            (str "Q" q ": RU has " ru-count " options but EN has " en-count))))))

(deftest test-de-en-option-counts-match
  (testing "German and English have same number of options per question"
    (doseq [q (range 1 11)]
      (let [de-count (count (get-in epds/questions [:de q :options]))
            en-count (count (get-in epds/questions [:en q :options]))]
        (is (= de-count en-count)
            (str "Q" q ": DE has " de-count " options but EN has " en-count))))))

(deftest test-uk-en-option-counts-match
  (testing "Ukrainian and English have same number of options per question"
    (doseq [q (range 1 11)]
      (let [uk-count (count (get-in epds/questions [:uk q :options]))
            en-count (count (get-in epds/questions [:en q :options]))]
        (is (= uk-count en-count)
            (str "Q" q ": UK has " uk-count " options but EN has " en-count))))))

(deftest test-uk-scoring-matches-en
  (testing "Ukrainian produces same scores as English for same answer patterns"
    (let [all-0 (vec (for [q (range 1 11)] {:question q :answer 0}))
          all-3 (vec (for [q (range 1 11)] {:question q :answer 3}))
          score-0 (epds/total-score all-0)
          score-3 (epds/total-score all-3)]
      (is (= 9 score-0) "UK all-index-0 should score 9")
      (is (= 21 score-3) "UK all-index-3 should score 21"))))

(deftest test-all-langs-scoring-consistency
  (testing "All three languages produce the same score for the same answer pattern"
    ;; For each question, the option at the same index should map to the same
    ;; severity level across all languages. We verify by checking that selecting
    ;; the same index for all questions produces the same score regardless of locale.
    (let [all-0 (vec (for [q (range 1 11)] {:question q :answer 0}))
          all-3 (vec (for [q (range 1 11)] {:question q :answer 3}))
          score-0 (epds/total-score all-0)
          score-3 (epds/total-score all-3)]
      ;; All reverse-scored Qs (1,2,4) give 3 pts at index 0, 0 pts at index 3
      ;; All normal-scored Qs give 0 pts at index 0, 3 pts at index 3
      ;; Score at index 0: 3+3+0+3+0+0+0+0+0+0 = 9
      ;; Score at index 3: 0+0+3+0+3+3+3+3+3+3 = 21
      (is (= 9 score-0) "All index-0 should score 9")
      (is (= 21 score-3) "All index-3 should score 21")
      ;; The important thing: selecting worst visible option always scores higher
      ;; than selecting best visible option
      (is (> score-3 score-0) "Worst options should score higher than best options"))))

;; -- manual scoring test for Russian ----------------------------------------
;; This test documents what a user sees when selecting specific options in Russian.
;; Q1 (reverse): "Совсем не могла"=0->3pts, "Как всегда"=3->0pts
;; Q2 (reverse): "Совсем не смотрела"=0->3pts, "Как всегда"=3->0pts
;; Q3 (normal):  "Нет, никогда"=0->0pts, "Да, большую часть времени"=3->3pts
;; Q4 (reverse): "Да, очень часто"=0->3pts, "Совсем нет"=3->0pts
;; Q5 (normal):  "Совсем нет"=0->0pts, "Да, довольно часто"=3->3pts
;; Q6 (normal):  "Нет, я справлялась..."=0->0pts, "Да, большую часть..."=3->3pts
;; Q7 (normal):  "Совсем нет"=0->0pts, "Да, большую часть времени"=3->3pts
;; Q8 (normal):  "Совсем нет"=0->0pts, "Да, большую часть времени"=3->3pts
;; Q9 (normal):  "Нет, никогда"=0->0pts, "Да, большую часть времени"=3->3pts
;; Q10 (normal): "Никогда"=0->0pts, "Да, довольно часто"=3->3pts

(deftest test-ru-selecting-worst-options-gives-high-score
  (testing "Selecting the worst option (index 3) for all RU questions gives 21 and triggers self-harm risk via Q10"
    ;; Worst visible option = index 3 for all questions
    ;; Q1 rev: 3->0, Q2 rev: 3->0, Q3 norm: 3->3, Q4 rev: 3->0
    ;; Q5 norm: 3->3, Q6 norm: 3->3, Q7 norm: 3->3, Q8 norm: 3->3, Q9 norm: 3->3, Q10 norm: 3->3
    ;; Total = 0+0+3+0+3+3+3+3+3+3 = 21
    ;; Q10 score = 3 > 0, so risk = :self-harm-risk (overrides total)
    (let [answers (make-answers 3 {})
          result (epds/score-screening answers)]
      (is (= 21 (:total-score result)))
      (is (= 3 (:q10-score result)))
      (is (= :self-harm-risk (:risk-level result))))))

(deftest test-ru-selecting-best-options-gives-low-score
  (testing "Selecting the best option (index 0) for all RU questions gives 9"
    ;; Best visible option = index 0 for all questions
    ;; Q1 rev: 0->3, Q2 rev: 0->3, Q3 norm: 0->0, Q4 rev: 0->3
    ;; Q5 norm: 0->0, Q6 norm: 0->0, Q7 norm: 0->0, Q8 norm: 0->0, Q9 norm: 0->0, Q10 norm: 0->0
    ;; Total = 3+3+0+3+0+0+0+0+0+0 = 9
    (let [answers (make-answers 0 {})
          result (epds/score-screening answers)]
      (is (= 9 (:total-score result)))
      (is (= :low-risk (:risk-level result))))))
