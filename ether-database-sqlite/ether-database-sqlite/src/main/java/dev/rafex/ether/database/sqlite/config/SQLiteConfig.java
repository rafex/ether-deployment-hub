package dev.rafex.ether.database.sqlite.config;

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

import java.util.Objects;

/**
 * Configuration for SQLite database connections.
 * <p>
 * Provides settings for journal mode, synchronous behavior, and other
 * SQLite-specific optimizations.
 */
public final class SQLiteConfig {

    private final JournalMode journalMode;
    private final SynchronousMode synchronousMode;
    private final boolean foreignKeys;
    private final int busyTimeout;
    private final boolean caseSensitiveLike;
    private final boolean recursiveTriggers;
    private final boolean autoVacuum;

    private SQLiteConfig(Builder builder) {
        this.journalMode = builder.journalMode;
        this.synchronousMode = builder.synchronousMode;
        this.foreignKeys = builder.foreignKeys;
        this.busyTimeout = builder.busyTimeout;
        this.caseSensitiveLike = builder.caseSensitiveLike;
        this.recursiveTriggers = builder.recursiveTriggers;
        this.autoVacuum = builder.autoVacuum;
    }

    /**
     * Returns the journal mode.
     *
     * @return journal mode, never {@code null}
     */
    public JournalMode journalMode() {
        return journalMode;
    }

    /**
     * Returns the synchronous mode.
     *
     * @return synchronous mode, never {@code null}
     */
    public SynchronousMode synchronousMode() {
        return synchronousMode;
    }

    /**
     * Returns whether foreign key constraints are enabled.
     *
     * @return {@code true} if foreign keys are enabled
     */
    public boolean foreignKeys() {
        return foreignKeys;
    }

    /**
     * Returns the busy timeout in milliseconds.
     *
     * @return busy timeout in milliseconds
     */
    public int busyTimeout() {
        return busyTimeout;
    }

    /**
     * Returns whether LIKE operator is case-sensitive.
     *
     * @return {@code true} if LIKE is case-sensitive
     */
    public boolean caseSensitiveLike() {
        return caseSensitiveLike;
    }

    /**
     * Returns whether recursive triggers are enabled.
     *
     * @return {@code true} if recursive triggers are enabled
     */
    public boolean recursiveTriggers() {
        return recursiveTriggers;
    }

    /**
     * Returns whether auto-vacuum is enabled.
     *
     * @return {@code true} if auto-vacuum is enabled
     */
    public boolean autoVacuum() {
        return autoVacuum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SQLiteConfig that = (SQLiteConfig) o;
        return foreignKeys == that.foreignKeys &&
               busyTimeout == that.busyTimeout &&
               caseSensitiveLike == that.caseSensitiveLike &&
               recursiveTriggers == that.recursiveTriggers &&
               autoVacuum == that.autoVacuum &&
               journalMode == that.journalMode &&
               synchronousMode == that.synchronousMode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalMode, synchronousMode, foreignKeys, busyTimeout,
                           caseSensitiveLike, recursiveTriggers, autoVacuum);
    }

    @Override
    public String toString() {
        return "SQLiteConfig{" +
               "journalMode=" + journalMode +
               ", synchronousMode=" + synchronousMode +
               ", foreignKeys=" + foreignKeys +
               ", busyTimeout=" + busyTimeout +
               ", caseSensitiveLike=" + caseSensitiveLike +
               ", recursiveTriggers=" + recursiveTriggers +
               ", autoVacuum=" + autoVacuum +
               '}';
    }

    /**
     * Creates a new builder with default values:
     * <ul>
     *   <li>Journal mode: {@link JournalMode#WAL}</li>
     *   <li>Synchronous mode: {@link SynchronousMode#NORMAL}</li>
     *   <li>Foreign keys: enabled ({@code true})</li>
     *   <li>Busy timeout: 5000ms</li>
     *   <li>Case-sensitive LIKE: disabled ({@code false})</li>
     *   <li>Recursive triggers: disabled ({@code false})</li>
     *   <li>Auto-vacuum: disabled ({@code false})</li>
     * </ul>
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the default configuration.
     *
     * @return default configuration
     */
    public static SQLiteConfig defaults() {
        return builder().build();
    }

    /**
     * Builder for {@link SQLiteConfig}.
     */
    public static final class Builder {
        private JournalMode journalMode = JournalMode.WAL;
        private SynchronousMode synchronousMode = SynchronousMode.NORMAL;
        private boolean foreignKeys = true;
        private int busyTimeout = 5000;
        private boolean caseSensitiveLike = false;
        private boolean recursiveTriggers = false;
        private boolean autoVacuum = false;

        private Builder() {}

        /**
         * Sets the journal mode.
         *
         * @param journalMode journal mode
         * @return this builder
         */
        public Builder journalMode(JournalMode journalMode) {
            this.journalMode = Objects.requireNonNull(journalMode, "journalMode must not be null");
            return this;
        }

        /**
         * Sets the synchronous mode.
         *
         * @param synchronousMode synchronous mode
         * @return this builder
         */
        public Builder synchronousMode(SynchronousMode synchronousMode) {
            this.synchronousMode = Objects.requireNonNull(synchronousMode, "synchronousMode must not be null");
            return this;
        }

        /**
         * Enables or disables foreign key constraints.
         *
         * @param foreignKeys {@code true} to enable foreign keys
         * @return this builder
         */
        public Builder foreignKeys(boolean foreignKeys) {
            this.foreignKeys = foreignKeys;
            return this;
        }

        /**
         * Sets the busy timeout in milliseconds.
         * <p>
         * When a database is locked, SQLite will retry for up to this duration
         * before returning {@code SQLITE_BUSY}.
         *
         * @param busyTimeout timeout in milliseconds, must be non-negative
         * @return this builder
         */
        public Builder busyTimeout(int busyTimeout) {
            if (busyTimeout < 0) {
                throw new IllegalArgumentException("busyTimeout must be non-negative");
            }
            this.busyTimeout = busyTimeout;
            return this;
        }

        /**
         * Enables or disables case-sensitive LIKE operator.
         *
         * @param caseSensitiveLike {@code true} for case-sensitive LIKE
         * @return this builder
         */
        public Builder caseSensitiveLike(boolean caseSensitiveLike) {
            this.caseSensitiveLike = caseSensitiveLike;
            return this;
        }

        /**
         * Enables or disables recursive triggers.
         *
         * @param recursiveTriggers {@code true} to enable recursive triggers
         * @return this builder
         */
        public Builder recursiveTriggers(boolean recursiveTriggers) {
            this.recursiveTriggers = recursiveTriggers;
            return this;
        }

        /**
         * Enables or disables auto-vacuum.
         *
         * @param autoVacuum {@code true} to enable auto-vacuum
         * @return this builder
         */
        public Builder autoVacuum(boolean autoVacuum) {
            this.autoVacuum = autoVacuum;
            return this;
        }

        /**
         * Builds the configuration.
         *
         * @return new SQLiteConfig instance
         */
        public SQLiteConfig build() {
            return new SQLiteConfig(this);
        }
    }
}