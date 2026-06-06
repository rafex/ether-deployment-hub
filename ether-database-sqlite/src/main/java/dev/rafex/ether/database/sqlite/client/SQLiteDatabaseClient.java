package dev.rafex.ether.database.sqlite.client;

/*-
 * #%L
 * ether-database-sqlite
 * %%
 * Copyright (C) 2025 - 2026 Raúl Eduardo González Argote
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.sql.DataSource;

import dev.rafex.ether.database.core.DatabaseClient;
import dev.rafex.ether.database.core.exceptions.DatabaseAccessException;
import dev.rafex.ether.database.core.mapping.ResultSetExtractor;
import dev.rafex.ether.database.core.mapping.RowMapper;
import dev.rafex.ether.database.core.sql.SqlQuery;
import dev.rafex.ether.database.core.sql.StatementBinder;
import dev.rafex.ether.database.core.transaction.TransactionCallback;
import dev.rafex.ether.database.sqlite.config.SQLiteConfig;
import dev.rafex.ether.database.sqlite.config.SQLitePragmas;
import dev.rafex.ether.database.sqlite.errors.SQLiteErrorClassifier;
import dev.rafex.ether.jdbc.client.JdbcDatabaseClient;

/**
 * SQLite-specific wrapper around {@link JdbcDatabaseClient}.
 * <p>
 * This class provides SQLite-specific configuration and error handling
 * while delegating all database operations to {@link JdbcDatabaseClient}.
 */
public class SQLiteDatabaseClient implements DatabaseClient {

    private final JdbcDatabaseClient delegate;
    private final Optional<SQLiteConfig> config;

    /**
     * Creates a new {@code SQLiteDatabaseClient} with the specified data source.
     *
     * @param dataSource the data source to use for database connections
     * @throws NullPointerException if {@code dataSource} is {@code null}
     */
    public SQLiteDatabaseClient(final DataSource dataSource) {
        this(new JdbcDatabaseClient(dataSource), Optional.empty());
    }

    /**
     * Creates a new {@code SQLiteDatabaseClient} with the specified data source
     * and configuration.
     *
     * @param dataSource the data source to use for database connections
     * @param config the SQLite configuration to apply
     * @throws NullPointerException if {@code dataSource} or {@code config} is {@code null}
     */
    public SQLiteDatabaseClient(final DataSource dataSource, final SQLiteConfig config) {
        this(new JdbcDatabaseClient(dataSource), Optional.of(Objects.requireNonNull(config, "config")));
    }

    private SQLiteDatabaseClient(final JdbcDatabaseClient delegate, final Optional<SQLiteConfig> config) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * Returns a builder for creating {@code SQLiteDatabaseClient} instances.
     *
     * @param dataSource the data source to use
     * @return a builder instance
     */
    public static Builder builder(final DataSource dataSource) {
        return new Builder(dataSource);
    }

    /**
     * Builder for {@link SQLiteDatabaseClient}.
     */
    public static final class Builder {
        private final DataSource dataSource;
        private SQLiteConfig config;

        private Builder(final DataSource dataSource) {
            this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
            this.config = SQLiteConfig.defaults();
        }

        /**
         * Enables Write-Ahead Logging (WAL) mode.
         *
         * @return this builder
         */
        public Builder withWalEnabled() {
            this.config = SQLiteConfig.builder()
                .journalMode(dev.rafex.ether.database.sqlite.config.JournalMode.WAL)
                .synchronousMode(config.synchronousMode())
                .foreignKeys(config.foreignKeys())
                .busyTimeout(config.busyTimeout())
                .caseSensitiveLike(config.caseSensitiveLike())
                .recursiveTriggers(config.recursiveTriggers())
                .autoVacuum(config.autoVacuum())
                .build();
            return this;
        }

        /**
         * Sets the synchronous mode.
         *
         * @param mode the synchronous mode
         * @return this builder
         */
        public Builder withSynchronousMode(final dev.rafex.ether.database.sqlite.config.SynchronousMode mode) {
            this.config = SQLiteConfig.builder()
                .journalMode(config.journalMode())
                .synchronousMode(mode)
                .foreignKeys(config.foreignKeys())
                .busyTimeout(config.busyTimeout())
                .caseSensitiveLike(config.caseSensitiveLike())
                .recursiveTriggers(config.recursiveTriggers())
                .autoVacuum(config.autoVacuum())
                .build();
            return this;
        }

        /**
         * Sets the journal mode.
         *
         * @param mode the journal mode
         * @return this builder
         */
        public Builder withJournalMode(final dev.rafex.ether.database.sqlite.config.JournalMode mode) {
            this.config = SQLiteConfig.builder()
                .journalMode(mode)
                .synchronousMode(config.synchronousMode())
                .foreignKeys(config.foreignKeys())
                .busyTimeout(config.busyTimeout())
                .caseSensitiveLike(config.caseSensitiveLike())
                .recursiveTriggers(config.recursiveTriggers())
                .autoVacuum(config.autoVacuum())
                .build();
            return this;
        }

        /**
         * Enables or disables foreign key constraints.
         *
         * @param enabled {@code true} to enable foreign keys, {@code false} to disable
         * @return this builder
         */
        public Builder withForeignKeys(final boolean enabled) {
            this.config = SQLiteConfig.builder()
                .journalMode(config.journalMode())
                .synchronousMode(config.synchronousMode())
                .foreignKeys(enabled)
                .busyTimeout(config.busyTimeout())
                .caseSensitiveLike(config.caseSensitiveLike())
                .recursiveTriggers(config.recursiveTriggers())
                .autoVacuum(config.autoVacuum())
                .build();
            return this;
        }

        /**
         * Sets the busy timeout in milliseconds.
         *
         * @param timeout the busy timeout in milliseconds
         * @return this builder
         */
        public Builder withBusyTimeout(final int timeout) {
            this.config = SQLiteConfig.builder()
                .journalMode(config.journalMode())
                .synchronousMode(config.synchronousMode())
                .foreignKeys(config.foreignKeys())
                .busyTimeout(timeout)
                .caseSensitiveLike(config.caseSensitiveLike())
                .recursiveTriggers(config.recursiveTriggers())
                .autoVacuum(config.autoVacuum())
                .build();
            return this;
        }

        /**
         * Sets a custom SQLite configuration.
         *
         * @param config the SQLite configuration
         * @return this builder
         */
        public Builder withConfig(final SQLiteConfig config) {
            this.config = Objects.requireNonNull(config, "config");
            return this;
        }

        /**
         * Builds the {@code SQLiteDatabaseClient}.
         *
         * @return a new {@code SQLiteDatabaseClient} instance
         */
        public SQLiteDatabaseClient build() {
            return new SQLiteDatabaseClient(dataSource, config);
        }
    }

    /**
     * Returns the SQLite configuration, if present.
     *
     * @return an optional containing the SQLite configuration, or empty if not configured
     */
    public Optional<SQLiteConfig> getConfig() {
        return config;
    }

    /**
     * Wraps a SQL exception with SQLite-specific error classification.
     *
     * @param message the error message
     * @param cause the SQL exception
     * @return a classified runtime exception
     */
    private RuntimeException wrapException(final String message, final SQLException cause) {
        // For now, just use DatabaseAccessException
        // In the future, we could add SQLite-specific exception types
        return new DatabaseAccessException(message, cause);
    }

    /**
     * Executes an operation with SQLite error handling.
     *
     * @param operation the database operation to execute
     * @param <T> the return type
     * @return the operation result
     */
    private <T> T executeWithErrorHandling(final DatabaseOperation<T> operation) {
        try {
            return operation.execute();
        } catch (DatabaseAccessException e) {
            // Re-throw with SQLite error classification if the cause is SQLException
            Throwable cause = e.getCause();
            if (cause instanceof SQLException) {
                throw wrapException(e.getMessage(), (SQLException) cause);
            }
            throw e;
        }
    }

    @Override
    public <T> T query(final SqlQuery query, final ResultSetExtractor<T> extractor) {
        return executeWithErrorHandling(() -> delegate.query(query, extractor));
    }

    @Override
    public <T> List<T> queryList(final SqlQuery query, final RowMapper<T> mapper) {
        return executeWithErrorHandling(() -> delegate.queryList(query, mapper));
    }

    @Override
    public <T> Optional<T> queryOne(final SqlQuery query, final RowMapper<T> mapper) {
        return executeWithErrorHandling(() -> delegate.queryOne(query, mapper));
    }

    @Override
    public int execute(final SqlQuery query) {
        return executeWithErrorHandling(() -> delegate.execute(query));
    }

    @Override
    public long[] batch(final String sql, final List<StatementBinder> binders) {
        return executeWithErrorHandling(() -> delegate.batch(sql, binders));
    }

    @Override
    public <T> T inTransaction(final TransactionCallback<T> callback) {
        return executeWithErrorHandling(() -> {
            // JdbcDatabaseClient doesn't implement TransactionRunner,
            // so we need to implement transaction handling ourselves
            // For now, throw UnsupportedOperationException
            throw new UnsupportedOperationException(
                "Transaction support not implemented in SQLiteDatabaseClient. " +
                "Use JdbcDatabaseClient directly for transaction support."
            );
        });
    }

    /**
     * Functional interface for database operations.
     */
    @FunctionalInterface
    private interface DatabaseOperation<T> {
        T execute();
    }
}