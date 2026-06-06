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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * Utility to apply SQLite PRAGMA settings to a connection.
 * <p>
 * This class helps configure SQLite connections with optimal settings
 * for performance, durability, and feature compatibility.
 */
public final class SQLitePragmas {

    private SQLitePragmas() {
        // Utility class
    }

    /**
     * Applies configuration to a SQLite connection.
     * <p>
     * Executes the necessary {@code PRAGMA} statements to configure the
     * connection according to the provided configuration.
     *
     * @param connection SQLite connection
     * @param config configuration to apply
     * @throws SQLException if any PRAGMA statement fails
     * @throws NullPointerException if {@code connection} or {@code config} is {@code null}
     */
    public static void apply(Connection connection, SQLiteConfig config) throws SQLException {
        Objects.requireNonNull(connection, "connection must not be null");
        Objects.requireNonNull(config, "config must not be null");

        try (Statement stmt = connection.createStatement()) {
            // Journal mode
            stmt.execute("PRAGMA journal_mode = " + config.journalMode().name());
            
            // Synchronous mode (can be integer or string)
            if (config.synchronousMode() == SynchronousMode.OFF ||
                config.synchronousMode() == SynchronousMode.NORMAL ||
                config.synchronousMode() == SynchronousMode.FULL ||
                config.synchronousMode() == SynchronousMode.EXTRA) {
                stmt.execute("PRAGMA synchronous = " + config.synchronousMode().toSql());
            } else {
                stmt.execute("PRAGMA synchronous = " + config.synchronousMode().pragmaValue());
            }
            
            // Foreign keys
            stmt.execute("PRAGMA foreign_keys = " + (config.foreignKeys() ? "ON" : "OFF"));
            
            // Busy timeout
            stmt.execute("PRAGMA busy_timeout = " + config.busyTimeout());
            
            // Case-sensitive LIKE
            stmt.execute("PRAGMA case_sensitive_like = " + (config.caseSensitiveLike() ? "1" : "0"));
            
            // Recursive triggers
            stmt.execute("PRAGMA recursive_triggers = " + (config.recursiveTriggers() ? "ON" : "OFF"));
            
            // Auto-vacuum
            stmt.execute("PRAGMA auto_vacuum = " + (config.autoVacuum() ? "1" : "0"));
            
            // Additional recommended pragmas
            stmt.execute("PRAGMA encoding = 'UTF-8'");
            stmt.execute("PRAGMA temp_store = MEMORY");
            stmt.execute("PRAGMA mmap_size = 268435456"); // 256MB
        }
    }

    /**
     * Applies default configuration to a SQLite connection.
     * <p>
     * Equivalent to {@code apply(connection, SQLiteConfig.defaults())}.
     *
     * @param connection SQLite connection
     * @throws SQLException if any PRAGMA statement fails
     * @throws NullPointerException if {@code connection} is {@code null}
     */
    public static void applyDefaults(Connection connection) throws SQLException {
        apply(connection, SQLiteConfig.defaults());
    }

    /**
     * Enables Write-Ahead Logging (WAL) mode on a connection.
     * <p>
     * This is a convenience method for enabling WAL mode specifically.
     *
     * @param connection SQLite connection
     * @throws SQLException if the PRAGMA statement fails
     * @throws NullPointerException if {@code connection} is {@code null}
     */
    public static void enableWal(Connection connection) throws SQLException {
        Objects.requireNonNull(connection, "connection must not be null");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode = WAL");
        }
    }

    /**
     * Disables Write-Ahead Logging (WAL) mode on a connection.
     * <p>
     * This is a convenience method for disabling WAL mode specifically.
     *
     * @param connection SQLite connection
     * @throws SQLException if the PRAGMA statement fails
     * @throws NullPointerException if {@code connection} is {@code null}
     */
    public static void disableWal(Connection connection) throws SQLException {
        Objects.requireNonNull(connection, "connection must not be null");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode = DELETE");
        }
    }

    /**
     * Sets the synchronous mode on a connection.
     * <p>
     * This is a convenience method for setting synchronous mode specifically.
     *
     * @param connection SQLite connection
     * @param mode synchronous mode
     * @throws SQLException if the PRAGMA statement fails
     * @throws NullPointerException if {@code connection} or {@code mode} is {@code null}
     */
    public static void setSynchronousMode(Connection connection, SynchronousMode mode) throws SQLException {
        Objects.requireNonNull(connection, "connection must not be null");
        Objects.requireNonNull(mode, "mode must not be null");
        
        try (Statement stmt = connection.createStatement()) {
            if (mode == SynchronousMode.OFF ||
                mode == SynchronousMode.NORMAL ||
                mode == SynchronousMode.FULL ||
                mode == SynchronousMode.EXTRA) {
                stmt.execute("PRAGMA synchronous = " + mode.toSql());
            } else {
                stmt.execute("PRAGMA synchronous = " + mode.pragmaValue());
            }
        }
    }
}