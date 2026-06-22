CREATE TABLE screenings (
  id              TEXT    PRIMARY KEY,
  created_at      TEXT    NOT NULL,
  locale          TEXT    NOT NULL DEFAULT 'ru',
  answers         TEXT    NOT NULL,
  total_score     INTEGER NOT NULL,
  q10_score       INTEGER NOT NULL,
  risk_level      TEXT    NOT NULL,
  age_range       TEXT,
  time_since_birth TEXT,
  first_child     TEXT,
  lat             REAL,
  lng             REAL,
  location_text   TEXT
);
