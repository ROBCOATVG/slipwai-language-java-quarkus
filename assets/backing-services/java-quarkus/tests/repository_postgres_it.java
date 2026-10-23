package com.example.deliverystarter.adapters.driven.repositorypostgres;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.deliverystarter.application.ports.state.Repository;
import com.example.deliverystarter.application.ports.state.SaveResult;
import com.example.deliverystarter.config.DatabaseUrl;
import com.example.deliverystarter.repositorycontract.RepositoryContract;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;

/**
 * The state repository against real Postgres.
 *
 * <p>Named {@code *IT}, so only {@code make test-integration} runs it:
 *
 * <pre>
 * make services-up migrate test-integration
 * </pre>
 *
 * <p>The shared contract, on the application's own datasource — Agroal, configured from {@code DATABASE_URL}
 * through {@code config/DatabaseUrlConfigSource} — and then the assertion no serialising store can
 * make: two independent connection pools racing one insert and one same-version update, each with exactly one
 * winner.
 */
@QuarkusTest
class PostgresRepositoryIT extends RepositoryContract {

    @Inject
    DataSource dataSource;

    @BeforeEach
    void requireTheSchema() {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.executeQuery("SELECT 1 FROM states LIMIT 1").close();
        } catch (SQLException failure) {
            throw new IllegalStateException(
                    "cannot read the states table: " + failure.getMessage()
                            + "\nStart the database and apply the schema first:  make services-up migrate",
                    failure);
        }
    }

    @Override
    protected Repository newRepository() {
        return new PostgresRepository(dataSource);
    }

    @Test
    void allowsExactlyOneWinnerAcrossTwoIndependentPools() throws Exception {
        Repository one = newRepository();
        Repository two = new PostgresRepository(independentPool());

        List<SaveResult> created = race(one, two, Repository.NO_STATE);
        assertThat(created).filteredOn(SaveResult.Saved.class::isInstance).hasSize(1);
        assertThat(created).filteredOn(SaveResult.VersionConflict.class::isInstance)
                .containsExactly(SaveResult.versionConflict(0));

        List<SaveResult> updated = race(one, two, 0);
        assertThat(updated).filteredOn(SaveResult.Saved.class::isInstance).hasSize(1);
        assertThat(updated).filteredOn(SaveResult.VersionConflict.class::isInstance)
                .containsExactly(SaveResult.versionConflict(1));

        String winner = updated.get(0) instanceof SaveResult.Saved ? "one" : "two";
        assertThat(one.load(id()).orElseThrow().state()).isEqualTo(Map.of("writer", winner));
    }

    /** A second pool the application never configured, so the race crosses connections it does not share. */
    private static DataSource independentPool() {
        DatabaseUrl database = DatabaseUrl.parse(System.getenv("DATABASE_URL"));
        PGSimpleDataSource pool = new PGSimpleDataSource();
        pool.setURL(database.jdbcUrl());
        pool.setUser(database.username());
        pool.setPassword(database.password());
        return pool;
    }
}
