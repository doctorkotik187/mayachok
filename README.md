# Маячок · Mayachok

**Open source postpartum depression screening for clinicians.**

Mayachok administers the Edinburgh Postnatal Depression Scale (EPDS) — a
validated 10-question clinical instrument used worldwide — and returns a
structured score with a clear recommendation a midwife or nurse can document.

It is not an AI. It does not generate advice. It runs a questionnaire and
returns a number. That number saves lives.

---

## Why

Postpartum depression affects roughly 1 in 6 mothers globally. In Russia,
[no systematic screening protocol exists](https://pmc.ncbi.nlm.nih.gov/articles/PMC10027014/)
and nearly half of all cases go undetected. Midwives report limited time and
limited tools for psychological assessment. Mayachok is a small attempt to fix that.

Built and launched in Saint Petersburg. Designed to work anywhere.

---

## Features

- Full EPDS in Russian and English
- Correct reverse scoring (questions 1, 2, 4)
- Automatic Q10 safety alert — always surfaces when self-harm ideation is indicated, regardless of total score
- Works offline as a Progressive Web App
- No PII stored by default
- Printable clinical report
- Single-file SQLite database — runs on a clinic laptop with no infrastructure

---

## Stack

- **Backend:** Clojure · Kit framework · Integrant · Reitit · next.jdbc · HugSQL · Migratus · SQLite
- **Frontend:** ClojureScript · Reagent · Re-frame · shadow-cljs
- **i18n:** tongue (Russian primary)

---

## Getting Started

Requires Java 11+ and the [Clojure CLI](https://clojure.org/guides/install_clojure).

```bash
git clone https://github.com/yourname/mayachok
cd mayachok
clj -M:dev:nrepl
```

```clojure
;; in the REPL
(go)   ; starts server on http://localhost:3000
```

---

## Contributing

Clinical contributions welcome — especially translation review, EPDS
validation citation checking, and UX feedback from practising midwives or
pharmacists.

All changes to scoring logic require a corresponding test. Changes to EPDS
question wording require a citation to the published validated translation.

See [`AGENTS.md`](./AGENTS.md) for full architecture and coding conventions.

---

## License

[AGPL-3.0](./LICENSE) — Any deployment that modifies this tool must publish
its changes. Clinical correctness must remain auditable by the community.

---

*"The first thousand days of a child's life begin with a healthy mother."*
