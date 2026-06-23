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
- Optional location input for regional aggregate statistics
- SQLite persistence for self-hosted deployments
- 4 languages: Russian, English, German, Ukrainian (`:ru` `:en` `:de` `:uk`)
- Bulma CSS, responsive, dark mode support
- Correct EPDS scoring with reverse-scored questions (Q1, Q2, 4)
- `/help` page (crisis hotlines, therapist directories, Telegram channels, AI chat prompt)
- PDF export of results
- Heatmap page (`/map`) showing regional screening data
- Aggregate statistics API (`/api/stats`) with risk breakdown, survey demographics, and regional data
- API health endpoint (`/api/health`) with version info
- Swagger API docs at `/api/api-docs/index.html`
- Version displayed in page footer
- Tests: 40 passing, covering scoring, risk levels, API, and health endpoint

**Not yet built:**
- [ ] PWA manifest / offline support
- [ ] Aggregate statistics dashboard (web UI, not just API)
- [ ] Docker image for easier self-hosting

---

## Project Structure

```
mayachok/
├── AGENTS.md                              ← you are here
├── README.md                              ← project overview and docs
├── VERSION                                ← current version (single source of truth)
├── deps.edn                               ← Clojure deps
├── build.clj                              ← uberjar build (reads VERSION)
├── resources/
│   ├── system.edn                         ← Integrant system config
│   ├── VERSION                            ← copied here for classpath access at runtime
│   ├── sql/
│   │   └── queries.sql                    ← ALL database queries (HugSQL)
│   ├── help-resources.edn                 ← Help page resources (editable by community)
│   └── migrations/                        ← SQL migration files (Migratus)
│       └── 20260620084501-create-screenings-table.up.sql
├── resources/public/                      ← Static assets served by Undertow
│   ├── favicon.png
│   ├── img/pink-sharky.png
│   └── css/screen.css                     ← Minimal custom CSS (animations, option cards)
├── resources/html/                        ← Selmer templates
│   ├── base.html                          ← HTML skeleton (Bulma CDN, version in footer)
│   ├── home.html                          ← Landing page
│   ├── question.html                      ← Single question (radio options, progress)
│   ├── result.html                        ← Score + risk + crisis alert + survey
│   ├── help.html                          ← Resources page (hotlines, chat, Telegram, AI prompt)
│   ├── map.html                           ← Heatmap of regional screening data
│   ├── thankyou.html                      ← Survey/region submission confirmation
│   └── error.html                         ← Error page
├── src/clj/mayachok/mayachok/
│   ├── core.clj                           ← System init, (go) / (halt) / (reset)
│   ├── config.clj                         ← Aero config loading
│   └── web/
│       ├── routes/
│       │   ├── api.clj                    ← API routes (screenings, stats, health)
│       │   └── pages.clj                  ← Page routes (landing, question, result, etc.)
│       ├── controllers/
│       │   ├── health.clj                 ← GET /api/health (with version)
│       │   └── screening.clj             ← POST/GET /api/screenings
│       ├── domain/
│       │   └── epds.clj                   ← Scoring logic + validated questions (RU/EN/DE/UK)
│       ├── middleware/                    ← Ring middleware stack
│       ├── pages/
│       │   └── layout.clj                 ← Selmer render + version injection + error page
│       └── i18n.clj                       ← Translation maps (RU/EN/DE/UK)
└── test/clj/mayachok/mayachok/
    ├── test_utils.clj                     ← Test helpers (system fixture, GET/POST)
    ├── domain/epds_test.clj               ← Scoring + risk level tests
    └── web/
        ├── api_test.clj                   ← Stats API + health endpoint tests
        └── request_test.clj               ← HTTP request integration tests
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

## Versioning

Version is maintained in the `VERSION` file at project root (also copied to `resources/` for runtime access). It's a single source of truth consumed by:

- `build.clj` — uberjar build
- `layout.clj` — page footer (`v0.1.0`)
- `health.clj` — `/api/health` endpoint
- `api.clj` — Swagger API spec

Bump by editing `VERSION`. Use SemVer: `0.x.x` for development, `1.0.0` for first stable release.

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

**Crisis resources (editable in `help-resources.edn`):**
- RU: 8-800-2000-122 (Телефон доверия), 004 (Санкт-Петербург)
- EN: 988 (Suicide & Crisis Lifeline, US)
- DE: 0800-1110111 (Telefonseelsorge)
- UK: 7333 (Ланінг), 0 800 500 225 (Міжнародний фонд України)

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
  patient_ref     TEXT,                    -- optional opaque reference
  lat             REAL,                    -- optional geolocation
  lng             REAL,                    -- optional geolocation
  location_text   TEXT                     -- optional, e.g. "Saint Petersburg, Russia"
);
```

No PII stored by default. `patient_ref` is an opaque clinic-assigned reference, never a name or date of birth.

---

## API Reference

### `GET /api/health`
Health check. Returns `{time, up_since, version, app: {status, message}}`.

### `POST /api/screenings`
Submit a completed screening. Body: `{answers: [{question: 1-10, answer: 0-3}, ...]}`. Returns `{total_score, q10_score, risk_level, id}`.

### `GET /api/screenings/:id`
Fetch a single screening by UUID.

### `GET /api/stats`
Aggregate statistics for medical professionals. Returns:
```json
{
  "total": 42,
  "avg_score": 12.3,
  "risk": {"low-risk": 10, "possible-depression": 15, "probable-depression": 12, "self-harm-risk": 5},
  "survey": {"age_range": {"25-34": 20}, "time_since_birth": {"0-6w": 10}, "first_child": {"t": 25, "f": 17}},
  "regions": [{"region": "Saint Petersburg, Russia", "total": 20, "avg_score": 11.5, "self_harm_count": 2, "lat": 59.93, "lng": 30.32}]
}
```

Available SQL queries in `resources/sql/queries.sql` — all HugSQL.

---

## Adding a Translation

Supported locales: `:ru`, `:en`, `:de`, `:uk`. To add a new language:

1. Add the locale key to `translations` in `i18n.clj` (same structure as existing locales)
2. Add EPDS questions to `questions` in `epds.clj` — validated translation only, with options ordered to match scoring direction
3. Add language-specific strings in `pages.clj` (risk labels, recommendations)
4. Add language switch link in `home.html`

---

## License

AGPL-3.0 — Any deployment that modifies this tool must publish changes.
Clinical correctness must remain auditable by the community.
