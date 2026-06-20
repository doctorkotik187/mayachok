-- :name create-screening! :! :n
-- :doc inserts a new screening record
INSERT INTO screenings (id, created_at, locale, mode, answers, total_score, q10_score, risk_level, clinic_id, patient_ref)
VALUES (:id, :created_at, :locale, :mode, :answers, :total_score, :q10_score, :risk_level, :clinic_id, :patient_ref)

-- :name get-screening-by-id :? :1
-- :doc fetches a single screening by UUID
SELECT * FROM screenings WHERE id = :id

-- :name list-screenings :? :*
-- :doc fetches all screenings, newest first
SELECT * FROM screenings ORDER BY created_at DESC
