-- :name clear-screenings! :! :n
-- :doc deletes all screening records (for testing)
DELETE FROM screenings

-- :name create-screening! :! :n
-- :doc inserts a new screening record
INSERT INTO screenings (id, created_at, locale, answers, total_score, q10_score, risk_level, age_range, time_since_birth, first_child, lat, lng, location_text)
VALUES (:id, :created_at, :locale, :answers, :total_score, :q10_score, :risk_level, :age_range, :time_since_birth, :first_child, :lat, :lng, :location_text)

-- :name get-screening-by-id :? :1
-- :doc fetches a single screening by UUID
SELECT * FROM screenings WHERE id = :id

-- :name list-screenings :? :*
-- :doc fetches all screenings, newest first
SELECT * FROM screenings ORDER BY created_at DESC

-- :name update-survey! :! :n
-- :doc updates the anonymous survey fields for a screening
UPDATE screenings SET age_range = :age_range, time_since_birth = :time_since_birth, first_child = :first_child WHERE id = :id

-- :name update-location! :! :n
-- :doc updates the geolocation fields for a screening
UPDATE screenings SET lat = :lat, lng = :lng, location_text = :location_text WHERE id = :id

-- :name list-screenings-stats :? :*
-- :doc lists all screenings without answers json, newest first
SELECT id, created_at, locale, total_score, q10_score, risk_level,
       age_range, time_since_birth, first_child,
       lat, lng, location_text
FROM screenings
ORDER BY created_at DESC

-- :name list-screenings-by-region :? :*
-- :doc screenings that have location data
SELECT id, created_at, locale, total_score, q10_score, risk_level,
       age_range, time_since_birth, first_child,
       lat, lng, location_text
FROM screenings
WHERE location_text IS NOT NULL AND location_text != ''
ORDER BY created_at DESC

-- :name list-screenings-by-risk :? :*
-- :doc screenings filtered by risk level
SELECT id, created_at, locale, total_score, q10_score, risk_level,
       age_range, time_since_birth, first_child,
       lat, lng, location_text
FROM screenings
WHERE risk_level = :risk_level
ORDER BY created_at DESC

-- :name heatmap-data :? :*
-- :doc aggregate data for the heatmap, grouped by normalized location
SELECT
  COALESCE(location_text, 'unknown') as region,
  COUNT(*) as total,
  ROUND(AVG(total_score), 1) as avg_score,
  SUM(CASE WHEN risk_level = 'self-harm-risk' THEN 1 ELSE 0 END) as self_harm_count,
  SUM(CASE WHEN risk_level = 'probable-depression' THEN 1 ELSE 0 END) as probable_count,
  SUM(CASE WHEN risk_level = 'possible-depression' THEN 1 ELSE 0 END) as possible_count,
  SUM(CASE WHEN risk_level = 'low-risk' THEN 1 ELSE 0 END) as low_count,
  AVG(lat) as avg_lat,
  AVG(lng) as avg_lng
FROM screenings
GROUP BY location_text
ORDER BY total DESC
