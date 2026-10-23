MINOR

**`java-quarkus` services on the durable event store get the family's seed door.** The event rung's `postgres` row
takes `SEED_DOOR` from `java` and the pom's exec plugin gains a `seed` execution naming `SeedMain`, so `make
seed-<service> FILE=<json>` appends a slice's given events through the event-store port, at the stream's current
version, as the system actor `systems/seed-door` under one correlation id per run, with an identity hashed from
stream, type and canonical data so a re-run appends nothing. With no `DATABASE_URL` it says so and exits 2. Never
an HTTP route: the door is a process with the store's credentials. `language.json` now requires `java` >=1.2,
which carries the door.

**Catch-up:** `slipwai language upgrade java-quarkus` beside `slipwai language upgrade java`, then `slipwai
migrate`.
