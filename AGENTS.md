# AGENTS.md — Mayachok

### Postpartum Depression Screening Platform

> **Маячок** means *beacon* in Russian. Born in Saint Petersburg. Libre and open source (AGPL-3.0).

---

## What This Is

A screening tool that administers the **Edinburgh Postnatal Depression Scale (EPDS)** — a validated 10-question clinical instrument used globally to detect postpartum depression. Returns a structured numeric score with a clear clinical outcome.

This is **not** a chatbot. It does not generate advice. It runs a validated questionnaire and returns a score. The score maps to one of four clinical outcomes defined by published medical literature.

**Key principles:**
- Anonymous by default — no PII stored
- Works on any device, in any browser, with or without internet
- Self-hostable — single uberjar, single SQLite file, zero external dependencies
- Respects user's system theme (light/dark mode)

---

## Stack

Clojure · Kit · Integrant · Reitit · HugSQL · SQLite · Selmer templates · Bulma CSS

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

- 10 questions, each scored 0–3. Total range: 0–30.
- Q1, Q2, Q4 are **reverse-scored** (score = 3 - index).
- Q10 is safety-critical: any score > 0 triggers `:self-harm-risk` regardless of total.

**Risk thresholds:**
| Score | Level | Action |
|-------|-------|--------|
| Q10 > 0 | self-harm-risk | Always flag, show crisis resources |
| ≥ 13 | probable-depression | Refer to psychiatrist |
| ≥ 10 | possible-depression | Follow up within 2 weeks |
| < 10 | low-risk | Routine follow-up |

---

## i18n

Supported locales: `:ru`, `:en`, `:de`, `:uk`. Translations live in `i18n.clj` (UI strings) and `epds.clj` (validated EPDS questions). To add a language, follow the same structure as existing locales.

---

## License

AGPL-3.0 — Any deployment that modifies this tool must publish changes.
Clinical correctness must remain auditable by the community.
