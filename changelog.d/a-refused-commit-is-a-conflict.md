PATCH

**A conditional append Postgres refuses at commit is answered as a condition conflict on Quarkus, not thrown.**
A SERIALIZABLE transaction Postgres could not order is refused by the commit, after the work returned, and
Narayana reported it as a `QuarkusTransactionException`, so `appendIf` threw instead of answering
`conditionConflict` and the simultaneous-appends integration test failed intermittently. `JtaTransactions` now
finds the driver's exception among the failure's causes and hands it back in the port's shape, which the store
already reads the SQLSTATE from.

**Catch-up:** `slipwai language upgrade java-quarkus`, then `slipwai migrate`; the service's `JtaTransactions`
changes.
