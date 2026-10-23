# Changelog

No releases yet. Release notes are recorded here newest first, one entry per released version; the entry being
written is `changelog.d/`.

## 1.2.0 — MINOR

**A conditional append Postgres refuses at commit is answered as a condition conflict on Quarkus, not thrown.**
A SERIALIZABLE transaction Postgres could not order is refused by the commit, after the work returned, and
Narayana reported it as a `QuarkusTransactionException`, so `appendIf` threw instead of answering
`conditionConflict` and the simultaneous-appends integration test failed intermittently. `JtaTransactions` now
finds the driver's exception among the failure's causes and hands it back in the port's shape, which the store
already reads the SQLSTATE from.

**Catch-up:** `slipwai language upgrade java-quarkus`, then `slipwai migrate`; the service's `JtaTransactions`
changes.

**`java-quarkus` services on the durable event store get the family's seed door.** The event rung's `postgres` row
takes `SEED_DOOR` from `java` and the pom's exec plugin gains a `seed` execution naming `SeedMain`, so `make
seed-<service> FILE=<json>` appends a slice's given events through the event-store port, at the stream's current
version, as the system actor `systems/seed-door` under one correlation id per run, with an identity hashed from
stream, type and canonical data so a re-run appends nothing. With no `DATABASE_URL` it says so and exits 2. Never
an HTTP route: the door is a process with the store's credentials. `language.json` now requires `java` >=1.2,
which carries the door.

**Catch-up:** `slipwai language upgrade java-quarkus` beside `slipwai language upgrade java`, then `slipwai
migrate`.

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

