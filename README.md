# Маячок · Mayachok

**Libre postpartum depression screening. Anonymous by default. Works everywhere.**

Mayachok administers the Edinburgh Postnatal Depression Scale (EPDS) — a
validated 10-question clinical instrument used worldwide — and returns a
structured score with clear guidance.

It is not an AI. It does not generate advice. It runs a questionnaire and
returns a number. That number can save lives.

**Version:** 0.1.0 · **License:** AGPL-3.0 — libre and open source, always.

---

## Why

Postpartum depression affects roughly 1 in 7 mothers. It often goes undetected
because nobody asks the right questions. Mayachok gives mothers a private,
low-friction way to screen themselves — and gives clinicians a fast, structured
tool for routine visits.

Designed to work anywhere — on any device, in any
language, with or without internet.

![Pink Sharky](resources/public/img/pink-sharky.png)

---

## What It Is

- **A screening tool**, not a diagnosis. It tells you whether to seek help.
- **Anonymous by default.** No names, no accounts, no tracking.
- **Self-hostable.** Run it on a clinic laptop, a Raspberry Pi, or a cloud server.
- **Multilingual.** Russian, English, German, Ukrainian — more welcome.
- **Open source.** AGPL-3.0. Fork it, modify it, publish your changes.

---

## Stack

- **Backend:** Clojure · Kit · Integrant · Reitit · HugSQL · SQLite
- **Frontend:** Server-rendered Selmer templates · Bulma CSS
- **i18n:** Validated EPDS translations (RU/EN/DE/UK)
- **Deploy:** Single uberjar, single SQLite file, zero external dependencies

---

## Getting Started

Requires Java 11+ and the [Clojure CLI](https://clojure.org/guides/install_clojure).

```bash
git clone https://github.com/doctorkotik187/mayachok
cd mayachok
clj -M:dev
```

Then in the REPL:

```clojure
(go)   ; starts server on http://localhost:3000
(halt) ; stops
(reset) ; reload after changes
```

---

## Features

- Full EPDS with correct scoring (reverse-scored Q1, Q2, Q4)
- Q10 safety alert — always surfaces for self-harm ideation regardless of total score
- Anonymous optional survey (age range, time since birth, first child)
- Optional location input for aggregate regional statistics
- SQLite persistence for self-hosted deployments
- Aggregate statistics API (`/api/stats`) for medical professionals
- Heatmap page showing regional screening data
- Crisis resources page (`/help`) with hotlines, chats, Telegram, AI prompt
- PDF export of screening results
- Beautiful, responsive UI via Bulma
- Dark mode support (respects system preference)
- 40 passing tests covering scoring, risk levels, and API

---

## Roadmap

- [x] `/help` page — crisis hotlines, therapist directories, Telegram, AI chat prompt
- [x] PDF export of screening results
- [x] Aggregate statistics API and heatmap page
- [x] Ukrainian translation
- [ ] PWA manifest for offline use
- [ ] Aggregate statistics dashboard (web UI, not just API)
- [ ] Docker image for easier self-hosting

---

## Contributing

Clinical contributions welcome — especially:
- Translation review and new languages
- EPDS validation citation checking
- UX feedback from practising midwives and mothers
- Crisis resource links for your region

All changes to scoring logic require a corresponding test. Changes to EPDS
question wording should cite the published validated translation.

See [`AGENTS.md`](./AGENTS.md) for full architecture and coding conventions.

---

## Self-Hosting for Clinics

1. Build the uberjar: `clojure -T:build uber`
2. Run it: `java -jar target/mayachok-standalone.jar`
3. The SQLite DB is created automatically in the working directory
4. Optional: set `JDBC_URL` env var for a custom DB path

The optional anonymous survey collects age range, time since birth, and whether
it's the first child. No PII is stored. Clinics can use `clinic_id` and
`patient_ref` fields for their own opaque patient references.

---

## API

- `POST /api/screenings` — submit a screening (returns score + risk level)
- `GET /api/screenings/:id` — fetch a single screening by ID
- `GET /api/stats` — aggregate statistics (risk breakdown, survey demographics, regional heatmap)
- `GET /api/health` — health check with version info
- Swagger docs at `/api/api-docs/index.html`

## Credits

- **Pink Sharky** — mascot and friend. Thank you for inspiring me to make this.
- **Bulma CSS** — for making the internet a little prettier.
- **EPDS** — originally developed by Cox, Holden & Sagovsky (1987). This implementation uses validated translations.

## License

[AGPL-3.0](./LICENSE) — Any deployment that modifies this tool must publish
its changes. Clinical correctness must remain auditable by the community.

---

*"The first thousand days of a child's life begin with a healthy mother."*
