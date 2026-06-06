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

/**
 * SQLite configuration and PRAGMA utilities.
 * <p>
 * This package provides classes for configuring SQLite database connections,
 * including Write-Ahead Logging (WAL) support, synchronous modes, and other
 * SQLite-specific optimizations.
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link dev.rafex.ether.database.sqlite.config.SQLiteConfig} - Configuration builder</li>
 *   <li>{@link dev.rafex.ether.database.sqlite.config.JournalMode} - Journal mode enum</li>
 *   <li>{@link dev.rafex.ether.database.sqlite.config.SynchronousMode} - Synchronous mode enum</li>
 *   <li>{@link dev.rafex.ether.database.sqlite.config.SQLitePragmas} - PRAGMA application utility</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * SQLiteConfig config = SQLiteConfig.builder()
 *     .journalMode(JournalMode.WAL)
 *     .synchronousMode(SynchronousMode.NORMAL)
 *     .foreignKeys(true)
 *     .busyTimeout(5000)
 *     .build();
 *
 * try (Connection conn = DriverManager.getConnection("jdbc:sqlite:test.db")) {
 *     SQLitePragmas.apply(conn, config);
 *     // Use connection...
 * }
 * }</pre>
 *
 * @see <a href="https://www.sqlite.org/pragma.html">SQLite PRAGMA Documentation</a>
 */
package dev.rafex.ether.database.sqlite.config;