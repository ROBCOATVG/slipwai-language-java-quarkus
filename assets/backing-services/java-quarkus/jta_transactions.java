package com.example.deliverystarter.adapters.driven.sql;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.narayana.jta.QuarkusTransactionException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.function.Supplier;
import javax.sql.DataSource;

/**
 * Transactions, the way Quarkus does them: JTA.
 *
 * <p>{@code QuarkusTransaction} is the framework's own programmatic entry point to the transaction manager
 * — the same one {@code @Transactional} uses — and Agroal is what makes it enough. Ask an Agroal datasource
 * for a connection inside an active transaction and you get <em>that transaction's</em> connection,
 * enlisted, with its commit owned by the transaction manager. So the only thing this class does is decide
 * whether to start a transaction or join one; nothing here holds a connection, and there is no
 * {@link ThreadLocal} anywhere in this project.
 *
 * <p>What that buys, and the reason it is worth a class of its own: a slice annotated {@code @Transactional}
 * gets the append and its own repository's writes in one transaction, with no wiring between them. An
 * {@code inline} read model is that annotation and nothing else.
 *
 * <p>A bean, unlike the stores themselves: there is exactly one transaction manager in an application, so
 * an injection point for it is unambiguous. Which event store the application uses is still a composition
 * decision the project takes for itself.
 */
@ApplicationScoped
public class JtaTransactions implements Transactions {

    /** The savepoint every nested block marks. One name is enough — see {@link #inSavepoint}. */
    private static final String SAVEPOINT = "unit_of_work";

    /** How far into a commit failure's causes the driver's exception is looked for; it sits four deep. */
    private static final int MAX_FAILURE_DEPTH = 16;

    private final DataSource dataSource;

    @Inject
    JtaTransactions(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public <T> T inTransaction(Isolation isolation, Supplier<T> work) {
        if (QuarkusTransaction.isActive()) {
            // Already in one — somebody else's unit of work, or an outer append. JTA has no nested
            // transactions, so the undo boundary is a JDBC savepoint on the enlisted connection.
            return inSavepoint(work);
        }
        try {
            return QuarkusTransaction.requiringNew().call(() -> {
                if (isolation == Isolation.SERIALIZABLE) {
                    askForSerialisable();
                }
                return work.get();
            });
        } catch (QuarkusTransactionException failure) {
            // A failure at COMMIT surfaces as Narayana's rollback rather than as the port's: a SERIALIZABLE
            // transaction Postgres could not order is refused by the commit, after the work returned. The store
            // reads the SQLSTATE off the port's exception to call that a condition conflict, so the driver's own
            // exception — which Narayana keeps as the cause of a suppressed `XAException` — is handed back in
            // the port's shape here.
            Optional<SQLException> driver = driverFailure(failure);
            if (driver.isPresent()) {
                throw Transactions.failed("commit", driver.get());
            }
            throw failure;
        }
    }

    /** The first SQLException among a failure's causes and suppressed exceptions, breadth first. */
    private static Optional<SQLException> driverFailure(Throwable failure) {
        Deque<Throwable> pending = new ArrayDeque<>();
        pending.add(failure);
        for (int seen = 0; !pending.isEmpty() && seen < MAX_FAILURE_DEPTH; seen++) {
            Throwable next = pending.remove();
            if (next instanceof SQLException sql) {
                return Optional.of(sql);
            }
            // The depth cap is what stops a circular chain — Narayana's report includes one — not a check here.
            Throwable cause = next.getCause();
            if (cause != null) {
                pending.add(cause);
            }
            for (Throwable suppressed : next.getSuppressed()) {
                pending.add(suppressed);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean isActive() {
        return QuarkusTransaction.isActive();
    }

    @Override
    public <T> T onConnection(String what, SqlWork<T> work) {
        // Closing this connection returns the handle, and inside a transaction that is all it does: the
        // physical connection stays enlisted until the transaction manager commits it.
        try (Connection connection = dataSource.getConnection()) {
            return work.run(connection);
        } catch (SQLException failure) {
            throw Transactions.failed(what, failure);
        }
    }

    /**
     * A savepoint, in SQL rather than through {@link Connection#setSavepoint}.
     *
     * <p>Not a preference: Agroal refuses {@code rollback} on an enlisted connection — "Attempting to
     * rollback while enlisted in a transaction" — and it refuses the savepoint overload with it, because
     * under JTA the transaction manager owns the transaction's end. Rolling back to a savepoint does not
     * end anything, so the statement the JDBC call would have sent is sent directly and the transaction
     * manager keeps everything it owns.
     *
     * <p>One name for every depth is safe, and simpler than a counter this class would have to keep per
     * transaction: Postgres resolves {@code ROLLBACK TO} and {@code RELEASE} to the <em>most recent</em>
     * savepoint of that name, and blocks nest, so the most recent is always the innermost. The release
     * after a rollback is what keeps that true — {@code ROLLBACK TO} leaves the savepoint standing, and a
     * stale one would shadow the enclosing block's.
     */
    private <T> T inSavepoint(Supplier<T> work) {
        run("mark a savepoint", "SAVEPOINT " + SAVEPOINT);
        // A flag and a finally rather than a catch of everything: what has to happen is "undo unless it
        // succeeded", and that is what this says.
        boolean done = false;
        try {
            T result = work.get();
            run("release a savepoint", "RELEASE SAVEPOINT " + SAVEPOINT);
            done = true;
            return result;
        } finally {
            if (!done) {
                run("undo a nested block",
                        "ROLLBACK TO SAVEPOINT " + SAVEPOINT,
                        "RELEASE SAVEPOINT " + SAVEPOINT);
            }
        }
    }

    private void run(String what, String... statements) {
        onConnection(what, connection -> {
            try (Statement statement = connection.createStatement()) {
                for (String sql : statements) {
                    statement.execute(sql);
                }
            }
            return Boolean.TRUE;
        });
    }

    /**
     * {@code SET TRANSACTION}, not {@code Connection.setTransactionIsolation}, and the difference matters
     * here in a way it does not under Spring.
     *
     * <p>This connection goes back to a pool the transaction manager owns, and nothing in a JTA transaction
     * gets a chance to reset the level afterwards: by the time the transaction has ended, the handle is
     * gone. {@code SET TRANSACTION} applies to the current transaction only, so there is nothing to reset
     * and no way to leave a pooled connection serialisable for the next caller. Postgres requires it before
     * the transaction's first statement, which is why this runs the moment the transaction opens.
     */
    private void askForSerialisable() {
        run("ask for serialisable isolation", "SET TRANSACTION ISOLATION LEVEL SERIALIZABLE");
    }
}
