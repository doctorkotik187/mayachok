# AGENTS.md — Mayachok

### Postpartum Depression Screening Platform

> **Маячок** means *beacon* in Russian. Born in Saint Petersburg. Libre and open source (AGPL-3.0).

---

## What This Is

A screening tool that administers the **Edinburgh Postnatal Depression Scale (EPDS)** — a validated 10-question instrument used globally to detect postpartum depression. It produces a structured numeric score with a clear clinical outcome.

This is **not** a chatbot. It does not generate advice. It runs a validated questionnaire and returns a score. The score maps to one of four clinical outcomes defined by published medical literature.

**Key principles:**
- Anonymous by default — no PII stored unless clinic explicitly opts in
- Works on any device, in any browser, with or without internet
- Self-hostable — single uberjar, single SQLite file, zero external dependencies
- Respects user's system theme (light/dark mode)

---

## Current State

**Working:**
- Landing page → 10-question EPDS flow → result page with score + risk level
- Q10 safety alert (always surfaces for self-harm ideation)
- Anonymous optional survey (age range, time since birth, first child)
- SQLite persistence for self-hosted deployments
- 3 languages: Russian, English, German
- Bulma CSS, responsive, dark mode support
- Correct EPDS scoring with reverse-scored questions (Q1, Q2, 4)
- Tests: 20 passing, covering scoring boundaries and risk levels

**Not yet built:**
- `/resources` page (crisis hotlines, therapist directories by country)
- PDF export of results
- Aggregate statistics page
- PWA manifest / offline support
- Additional languages

---

## Project Structure

```
mayachok/
├── AGENTS.md                              ← you are here
├── README.md                              ← project overview and docs
├── deps.edn                               ← Clojure deps (kit-sql + sqlite added)
├── build.clj                              ← uberjar build (clojure -T:build uber)
├── resources/
│   ├── system.edn                         ← Integrant system config
│   ├── sql/
│   │   └── queries.sql                    ← ALL database queries (HugSQL)
│   └── migrations/                        ← SQL migration files (Migratus)
│       └── 20260620084501-create-screenings-table.up.sql
├── resources/public/                      ← Static assets served by Undertow
│   ├── favicon.png
│   ├── img/pink-sharky.png
│   └── css/screen.css                     ← Minimal custom CSS (animations, option cards)
├── resources/html/                        ← Selmer templates
│   ├── base.html                          ← HTML skeleton (Bulma CDN, blocks)
│   ├── home.html                          ← Landing page
│   ├── question.html                      ← Single question (radio options, progress)
│   ├── result.html                        ← Score + risk + crisis alert + survey
│   └── error.html                         ← Error page
├── src/clj/mayachok/mayachok/
│   ├── core.clj                           ← System init, (go) / (halt) / (reset)
│   ├── config.clj                         ← Aero config loading
│   └── web/
│       ├── routes/
│       │   ├── api.clj                    ← API routes (POST/GET screenings)
│       │   └── pages.clj                  ← Page routes (landing, question, result)
│       ├── controllers/
│       │   ├── health.clj                 ← GET /api/health
│       │   └── screening.clj             ← POST/GET /api/screenings
│       ├── middleware/                    ← Ring middleware stack
│       ├── pages/
│       │   └── layout.clj                 ← Selmer render + error page
│       └── i18n.clj                       ← Translation maps (RU/EN/DE)
└── test/clj/mayachok/mayachok/
    └── domain/epds_test.clj               ← Scoring + risk level tests
```

---

## How to Run

```bash
clj -M:dev        # start dev server
clj -M:test        # run tests
clojure -T:build uber  # build production uberjar
```

In the REPL: `(go)` / `(halt)` / `(reset)`

---

## EPDS Scoring

- 10 questions, each scored 0–3
- Total range: 0–30
- Q1, Q2, Q4 are **reverse-scored** (the option text goes from positive to negative, so the index is inverted: score = 3 - index)
- Q10 is safety-critical: any score > 0 triggers `:self-harm-risk` regardless of total

**Risk thresholds:**
| Score | Level | Action |
|-------|-------|--------|
| Q10 > 0 | self-harm-risk | Always flag, show crisis resources |
| ≥ 13 | probable-depression | Refer to psychiatrist |
| ≥ 10 | possible-depression | Follow up within 2 weeks |
| < 10 | low-risk | Routine follow-up |

**Crisis resources (hardcoded, never fetched from API):**
- RU: 8-800-2000-122 (Телефон доверия), 004 (Санкт-Петербург)
- EN: 988 (Suicide & Crisis Lifeline, US)
- DE: 0800-1110111 (Telefonseelsorge)

---

## Data Model

```sql
CREATE TABLE screenings (
  id              TEXT PRIMARY KEY,       -- UUID v4, generated server-side
  created_at      TEXT NOT NULL,          -- ISO 8601 UTC timestamp
  locale          TEXT NOT NULL DEFAULT 'ru',
  mode            TEXT NOT NULL DEFAULT 'self',  -- 'self' | 'clinician'
  answers         TEXT NOT NULL,           -- JSON: [{:q 1 :a 2} ...]
  total_score     INTEGER NOT NULL,
  q10_score       INTEGER NOT NULL,
  risk_level      TEXT NOT NULL,
  age_range       TEXT,                    -- optional survey
  time_since_birth TEXT,                   -- optional survey
  first_child     TEXT,                    -- optional survey
  clinic_id       TEXT,                    -- optional, for multi-clinic
  patient_ref     TEXT                     -- optional opaque reference
);
```

No PII stored by default. `patient_ref` is an opaque clinic-assigned reference, never a name or date of birth.

---

## Adding a Translation

1. Add the locale key to `translations` in `i18n.clj` (same structure as `:ru` and `:en`)
2. Add EPDS questions to `questions` in `epds.clj` — validated translation only, with options ordered to match scoring direction
3. Add risk labels and recommendations in `pages.clj`
4. Add language switch link in `home.html`

---

## License

AGPL-3.0 — Any deployment that modifies this tool must publish changes.
Clinical correctness must remain auditable by the community.
