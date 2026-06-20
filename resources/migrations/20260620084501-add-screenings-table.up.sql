CREATE TABLE screenings (
  id          TEXT    PRIMARY KEY,
  created_at  TEXT    NOT NULL,
  locale      TEXT    NOT NULL DEFAULT 'ru',
  mode        TEXT    NOT NULL DEFAULT 'clinician',
  answers     TEXT    NOT NULL,
  total_score INTEGER NOT NULL,
  q10_score   INTEGER NOT NULL,
  risk_level  TEXT    NOT NULL,
  clinic_id   TEXT,
  patient_ref TEXT
);
