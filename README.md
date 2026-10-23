# slipwai-language-java-quarkus
slipwai 2.0 language addon: slipwai-language-java-quarkus

## Write models

`--write-model events` and `--write-model state` are both answered, over `--persistence memory|sqlite|postgres`.
The state rung takes the family's repository port and adapters (see `slipwai-language-java`), this framework's
`DATABASE_URL` wiring and `PostgresRepositoryIT`, a `@QuarkusTest` that runs the shared contract on the
application's pool and races two independent pools for one row. Against a real database:

    make services-up migrate test-integration
