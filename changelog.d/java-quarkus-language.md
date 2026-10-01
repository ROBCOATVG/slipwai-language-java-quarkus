MINOR

**The Quarkus backend is now a framework package of the `java` family.** The `java-quarkus` backend that was
built into slipwai — its pom and properties, its walking skeleton, its Quarkus adapters (the datasource's address,
the health check, the 404 mapper, Keycloak's group mapping, the scheduler, JTA) and its gate — lives here, with the
history it had there. Everything Java shares comes from the `java` package, which this one requires
(`requires: {"java": ">=1.0,<2"}`); without it, or with a `java` outside that range, slipwai refuses this package
alone. The projects it generates are byte for byte those slipwai generated with Java built in. It declares the
catalog schema it loads on, `core >=9.0,<10`.
