-- :name create-screening! :! :n
-- :doc inserts a new screening record
INSERT INTO screenings (id, created_at, locale, answers, total_score, q10_score, risk_level, age_range, time_since_birth, first_child, region_code)
VALUES (:id, :created_at, :locale, :answers, :total_score, :q10_score, :risk_level, :age_range, :time_since_birth, :first_child, :region_code)

-- :name get-screening-by-id :? :1
-- :doc fetches a single screening by UUID
SELECT * FROM screenings WHERE id = :id

-- :name list-screenings :? :*
-- :doc fetches all screenings, newest first
SELECT * FROM screenings ORDER BY created_at DESC

-- :name update-survey! :! :n
-- :doc updates the anonymous survey fields for a screening
UPDATE screenings SET age_range = :age_range, time_since_birth = :time_since_birth, first_child = :first_child WHERE id = :id

-- :name update-region! :! :n
-- :doc updates the region_code for a screening
UPDATE screenings SET region_code = :region_code WHERE id = :id

-- :name heatmap-data :? :*
-- :doc aggregate data for the heatmap: region, count, avg score, risk breakdown
SELECT
  region_code,
  COUNT(*) as total,
  ROUND(AVG(total_score), 1) as avg_score,
  SUM(CASE WHEN risk_level = 'self-harm-risk' THEN 1 ELSE 0 END) as self_harm_count,
  SUM(CASE WHEN risk_level = 'probable-depression' THEN 1 ELSE 0 END) as probable_count,
  SUM(CASE WHEN risk_level = 'possible-depression' THEN 1 ELSE 0 END) as possible_count,
  SUM(CASE WHEN risk_level = 'low-risk' THEN 1 ELSE 0 END) as low_count
FROM screenings
WHERE region_code IS NOT NULL
GROUP BY region_code
ORDER BY total DESC
