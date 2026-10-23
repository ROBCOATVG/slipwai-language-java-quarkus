# Changelog

No releases yet. Release notes are recorded here newest first, one entry per released version; the entry being
written is `changelog.d/`.

## 1.1.0 — MINOR

**`java-quarkus` answers the state-stored rung: `--write-model state` generates a service on a versioned
repository, over memory, SQLite or Postgres.** `write_side_files` gains a `state` block built from the family's
shared rows, this framework's `config/DatabaseUrlConfigSource` wiring for `DATABASE_URL`, its transport and
identity rows as they are, and `PostgresRepositoryIT`: the shared contract on the application's Agroal pool under
`@QuarkusTest`, plus two independent pools racing one insert and one same-version update with exactly one winner
each. A state service has no projections and no checkpoint, so the read side answers nothing for the rung.
`language.json` offers the axis as `persistence`, its name in the core, and offers `write-model` `events` and
`state`; it now requires `java` >=1.1, which carries the shared rows.

**Catch-up:** `slipwai language upgrade java-quarkus` beside `slipwai language upgrade java`. An event-sourced
service is generated as before.

## 1.0.0

**The Quarkus backend is now a framework package of the `java` family.** The `java-quarkus` backend that was
built into slipwai — its pom and properties, its walking skeleton, its Quarkus adapters (the datasource's address,
the health check, the 404 mapper, Keycloak's group mapping, the scheduler, JTA) and its gate — lives here, with the
history it had there. Everything Java shares comes from the `java` package, which this one requires
(`requires: {"java": ">=1.0,<2"}`); without it, or with a `java` outside that range, slipwai refuses this package
alone. The projects it generates are byte for byte those slipwai generated with Java built in. It declares the
catalog schema it loads on, `core >=9.0,<10`.

