# AGENTS.md — Mayachok (Маячок)
### Postpartum Depression Screening Platform

> **Маячок** means *beacon* in Russian. Born in Saint Petersburg.

---

## What This Is

A clinical screening tool that administers the **Edinburgh Postnatal Depression
Scale (EPDS)** — a validated 10-question instrument used globally to detect
postpartum depression. It produces a structured numeric score with a clear
clinical recommendation that a midwife or nurse can document in a patient record.

This is **not** a chatbot. It does not generate advice. It runs a validated
questionnaire and returns a score. The score maps to one of four clinical
outcomes defined by published medical literature.

---

## Current State of the Project

This project was bootstrapped with:

```bash
neil new io.github.kit-clj/kit mayachok/mayachok
# then in the REPL:
(kit/install-module :kit/sql)   # installs SQLite + next.jdbc + HugSQL + Migratus
```

The `:kit/sql` module defaults to **SQLite** and has already configured:
- `resources/system.edn` — Integrant system with `:db.sql/connection`,
  `:db.sql/query-fn`, `:db.sql/migrations`
- `deps.edn` — added `io.github.kit-clj/kit-sql` and `org.xerial/sqlite-jdbc`
- `resources/sql/queries.sql` — HugSQL query file (currently empty, add queries here)
- `src/clj/mayachok/core.clj` — updated with `[kit.edge.db.sql]` require

**The ClojureScript frontend has not been installed yet.** When ready, run in REPL:
```clojure
(kit/install-module :kit/cljs)   ; adds shadow-cljs, Reagent
```

---

## Project Structure (Actual)

```
mayachok/
├── AGENTS.md                          ← you are here
├── deps.edn                           ← Clojure deps (kit-sql + sqlite added)
├── build.clj                          ← uberjar build (clojure -T:build uber)
├── kit.edn                            ← Kit module config + module repo
├── resources/
│   ├── system.edn                     ← Integrant system config — the nerve centre
│   ├── sql/
│   │   └── queries.sql                ← ALL database queries live here (HugSQL)
│   └── migrations/                    ← SQL migration files (Migratus)
│       └── (add timestamped .sql files here)
├── src/clj/mayachok/
│   ├── core.clj                       ← System init, (go) / (halt) / (reset)
│   ├── config.clj                     ← Aero config loading
│   └── web/
│       ├── routes.clj                 ← Top-level router (add route namespaces here)
│       ├── middleware.clj             ← Ring middleware stack
│       ├── middleware/
│       │   └── exception.clj         ← Exception handling middleware
│       └── controllers/
│           └── health.clj            ← GET /api/health (example controller)
├── env/
│   ├── dev/
│   │   ├── clj/mayachok/env.clj      ← Dev-only system config
│   │   └── resources/dev-config.edn  ← Dev profile values
│   ├── prod/
│   │   └── resources/prod-config.edn ← Prod profile values
│   └── test/
│       └── resources/test-config.edn ← Test profile values
├── modules/                           ← Downloaded Kit modules (do not edit)
└── test/clj/mayachok/                 ← Tests go here
```

---

## How to Run

### Start the dev REPL

```bash
clj -M:dev:nrepl
```

### Inside the REPL

```clojure
(go)      ;; starts the system (server + db) on port 3000
(halt)    ;; stops the system
(reset)   ;; halt + reload namespaces + go — use after code changes
```

### Run tests

```bash
clj -M:test
```

### Build production uberjar

```bash
clojure -T:build uber
java -jar target/mayachok-standalone.jar
```

---

## How the System Works (Integrant)

Kit uses **Integrant** for system lifecycle. The entire system is declared in
`resources/system.edn`. Every component (server, db connection, query function,
migrations) is a key in this map and can reference other keys via `#ig/ref`.

After `:kit/sql` install, `system.edn` contains roughly:

```edn
{:db.sql/connection
 #profile {:dev  {:jdbc-url "jdbc:sqlite:_dev.db"}
           :test {:jdbc-url "jdbc:sqlite:_test.db"}
           :prod {:jdbc-url #env JDBC_URL}}

 :db.sql/query-fn
 {:conn      #ig/ref :db.sql/connection
  :options   {}
  :filename  "sql/queries.sql"}

 :db.sql/migrations
 {:store          :database
  :db             {:datasource #ig/ref :db.sql/connection}
  :migrate-on-init? true}

 :server/undertow
 {...}

 :router/core
 {:routes #ig/ref :router/routes}}
```

**Never manage database connections manually.** Always inject `:db.sql/query-fn`
via Integrant into handlers that need DB access.

---

## How to Add a New Route

1. Create `src/clj/mayachok/web/controllers/screening.clj`
2. Define your handler functions there
3. Add the route to `src/clj/mayachok/web/routes.clj`

Example route entry:

```clojure
["/api/screenings"
 {:post {:handler mayachok.web.controllers.screening/create-screening!}}]

["/api/screenings/:id"
 {:get {:handler mayachok.web.controllers.screening/get-screening}}]
```

The query-fn is accessed inside handlers via the Integrant state. Kit injects
it into the request map — access it as `(get-in request [:state :db.sql/query-fn])`.

---

## How to Add Database Queries (HugSQL)

All queries live in `resources/sql/queries.sql` as HugSQL-annotated SQL.

```sql
-- :name create-screening! :! :n
-- :doc inserts a new screening record
INSERT INTO screenings (id, created_at, locale, answers, total_score, q10_score, risk_level)
VALUES (:id, :created_at, :locale, :answers, :total_score, :q10_score, :risk_level)

-- :name get-screening-by-id :? :1
-- :doc fetches a single screening by UUID
SELECT * FROM screenings WHERE id = :id

-- :name list-screenings :? :*
-- :doc fetches all screenings, newest first
SELECT * FROM screenings ORDER BY created_at DESC
```

HugSQL annotation format: `:name <fn-name> :<command> :<result>`
- Commands: `:!` (execute), `:?` (query)
- Results: `:n` (rows affected), `:1` (single row), `:*` (all rows)

---

## How to Add a Migration

Create a file in `resources/migrations/` following the Migratus naming convention:

```
resources/migrations/
  20240101000000-create-screenings.up.sql
  20240101000000-create-screenings.down.sql
```

Migratus runs migrations automatically on system start (`migrate-on-init? true`).

---

## The Clinical Domain — Read This Carefully

### What is the EPDS?

The Edinburgh Postnatal Depression Scale is a 10-item self-report questionnaire.
Each item is scored 0–3. Total range: 0–30.
It is validated, published, and used in clinical settings worldwide.

**Use the validated Russian translation exactly as published. Do not paraphrase
or simplify the question wording. The exact wording is what clinicians expect.**

### Scoring Rules

| Question | Topic                        | Scoring Direction |
|----------|------------------------------|-------------------|
| 1        | Ability to laugh             | **Reverse**       |
| 2        | Looking forward with joy     | **Reverse**       |
| 3        | Blaming self                 | Normal            |
| 4        | Anxious without good reason  | **Reverse**       |
| 5        | Scared or panicky            | Normal            |
| 6        | Things getting on top of you | Normal            |
| 7        | Difficulty sleeping          | Normal            |
| 8        | Feeling sad or miserable     | Normal            |
| 9        | Crying                       | Normal            |
| 10       | Thought of harming self      | Normal ⚠️         |

For **reverse-scored questions** (1, 2, 4), the answer options are presented
in the same order but scored in reverse: if the answer index is 0,1,2,3
then the score is 3,2,1,0.

```clojure
(def reverse-scored #{1 2 4})

(defn score-answer [question-number answer-index]
  (if (reverse-scored question-number)
    (- 3 answer-index)
    answer-index))

(defn total-score [answers]
  (->> answers
       (map (fn [{:keys [question answer]}]
              (score-answer question answer)))
       (reduce +)))
```

### Clinical Thresholds

```clojure
(defn risk-level [total q10]
  (cond
    (pos? q10)       :self-harm-risk        ;; Q10 > 0: ALWAYS flag, regardless of total
    (>= total 13)    :probable-depression   ;; Refer to psychiatrist
    (>= total 10)    :possible-depression   ;; Follow up within 2 weeks
    :else            :low-risk))            ;; Routine follow-up
```

### ⚠️ Q10 is Safety-Critical

Question 10 asks about thoughts of self-harm. If a mother scores **any points
on Q10** (answer index > 0), a safety alert MUST be shown regardless of the
total score. A mother could score 4 overall but if Q10 > 0, it must surface.

**This behaviour must never be changed without clinical review.**

### Crisis Resources (hardcode these, never fetch from an API)

```clojure
(def crisis-resources
  {:ru {:phone "8-800-2000-122"
        :label "Телефон доверия (бесплатно)"
        :spb   "004 — Экстренная психологическая помощь, Санкт-Петербург"}
   :en {:phone "988"
        :label "Suicide & Crisis Lifeline (US)"}})
```

These must render even when the app is offline.

---

## Data Model

### `screenings` table

```sql
CREATE TABLE screenings (
  id          TEXT    PRIMARY KEY,   -- UUID v4, generated server-side
  created_at  TEXT    NOT NULL,      -- ISO 8601 UTC timestamp
  locale      TEXT    NOT NULL DEFAULT 'ru',
  mode        TEXT    NOT NULL DEFAULT 'clinician', -- 'clinician' | 'self'
  answers     TEXT    NOT NULL,      -- JSON: [{:q 1 :a 2} ...]
  total_score INTEGER NOT NULL,
  q10_score   INTEGER NOT NULL,      -- stored separately, safety critical
  risk_level  TEXT    NOT NULL,      -- 'low-risk' | 'possible-depression' | 'probable-depression' | 'self-harm-risk'
  clinic_id   TEXT,                  -- optional, for multi-clinic deployments
  patient_ref TEXT                   -- optional clinic reference, never a real name
);
```

No PII is stored by default. `patient_ref` is an opaque clinic-assigned
reference (e.g. "2024-SPB-0042"), never a name or date of birth.

---

## Planned Namespaces to Create

These do not exist yet. Create them in this order:

### 1. `mayachok.domain.epds` (pure functions, no I/O)
- EPDS question definitions (both `ru` and `en`)
- `score-answer` — handles reverse scoring
- `total-score` — sums all answers
- `risk-level` — maps score + q10 to clinical outcome
- `crisis-resources` — hardcoded, locale-aware

### 2. `mayachok.web.controllers.screening`
- `create-screening!` — POST /api/screenings
  - Validates input with clojure.spec
  - Calls domain functions to compute score
  - Persists to DB
  - Returns `{:id, :total-score, :risk-level, :q10-score}`
- `get-screening` — GET /api/screenings/:id

### 3. ClojureScript frontend (after `(kit/install-module :kit/cljs)`)
- `mayachok-fe.core` — Re-frame init
- `mayachok-fe.events` — Re-frame events
- `mayachok-fe.subs` — Re-frame subscriptions
- `mayachok-fe.views.screening` — Question-by-question flow
- `mayachok-fe.views.result` — Score display + recommendation
- `mayachok-fe.i18n` — tongue translations (ru primary)

---

## Coding Conventions

- **Pure functions for all clinical logic.** `mayachok.domain.epds` has zero
  side effects. No DB calls, no HTTP. Pure input → output.
- **Spec all clinical inputs.** Validate answer payloads with `clojure.spec`
  before scoring. A malformed payload must never silently produce a score.
- **No magic numbers.** Score thresholds (10, 13) are named constants in
  `mayachok.domain.epds`, not inline integers.
- **HugSQL only for DB.** No SQL strings in Clojure files. All queries in
  `resources/sql/queries.sql`.
- **Answers stored as JSON.** Serialise answer vectors to JSON string before
  insert, deserialise after fetch. Use `jsonista` (already a Kit transitive dep).
- **UUIDs server-side only.** Never trust a client-supplied ID. Generate with
  `java.util.UUID/randomUUID` in the handler.
- **Test scoring exhaustively.** All-zeros, all-threes, Q10=1 with low total,
  each reverse-scored question, boundary scores (9, 10, 12, 13). Clinical
  correctness is the highest priority in this codebase.

---

## What to Build Next (in order)

- [ ] Migration: `create-screenings` table
- [ ] HugSQL queries: `create-screening!`, `get-screening-by-id`
- [ ] `mayachok.domain.epds` with full EPDS logic and tests
- [ ] `mayachok.web.controllers.screening` with POST + GET handlers
- [ ] Wire screening routes into `routes.clj`
- [ ] Install ClojureScript: `(kit/install-module :kit/cljs)`
- [ ] Re-frame app: question flow → submit → result screen
- [ ] Russian translations for all EPDS questions and UI strings
- [ ] Q10 safety alert component (hardcoded crisis resources)
- [ ] PWA manifest + service worker for offline support
- [ ] Printable result page (CSS print stylesheet or PDF export)

---

## License

AGPL-3.0 — Any deployment that modifies this tool must publish changes.
Clinical correctness must remain auditable by the community.
