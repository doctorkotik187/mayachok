-- :name create-screening! :! :n
-- :doc inserts a new screening record
INSERT INTO screenings (id, created_at, locale, mode, answers, total_score, q10_score, risk_level, age_range, time_since_birth, first_child, clinic_id, patient_ref)
VALUES (:id, :created_at, :locale, :mode, :answers, :total_score, :q10_score, :risk_level, :age_range, :time_since_birth, :first_child, :clinic_id, :patient_ref)

-- :name get-screening-by-id :? :1
-- :doc fetches a single screening by UUID
SELECT * FROM screenings WHERE id = :id

-- :name list-screenings :? :*
-- :doc fetches all screenings, newest first
SELECT * FROM screenings ORDER BY created_at DESC

-- :name update-survey! :! :n
-- :doc updates survey fields for a screening
UPDATE screenings SET age_range = :age_range, time_since_birth = :time_since_birth, first_child = :first_child WHERE id = :id

-- :name survey-stats :? :*
-- :doc aggregate statistics for the survey page
SELECT
  COUNT(*) as total,
  risk_level,
  age_range,
  time_since_birth,
  first_child,
  locale,
  ROUND(AVG(total_score), 1) as avg_score,
  SUM(CASE WHEN q10_score > 0 THEN 1 ELSE 0 END) as q10_count
FROM screenings
GROUP BY risk_level, age_range, time_since_birth, first_child, locale
ORDER BY total DESC
