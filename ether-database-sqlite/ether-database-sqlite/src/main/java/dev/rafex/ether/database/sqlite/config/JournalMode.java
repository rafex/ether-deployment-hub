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

/**
 * SQLite journal modes.
 * <p>
 * Corresponds to the {@code PRAGMA journal_mode} setting.
 *
 * @see <a href="https://www.sqlite.org/pragma.html#pragma_journal_mode">PRAGMA journal_mode</a>
 */
public enum JournalMode {
    
    /**
     * Write-Ahead Logging mode.
     * <p>
     * Allows concurrent reads and writes, with better performance for
     * write-intensive workloads. The default and recommended mode for
     * most applications.
     */
    WAL,
    
    /**
     * Rollback journal mode (default in SQLite &lt; 3.7.0).
     * <p>
     * Creates a separate rollback journal file ({@code -journal}) that is
     * deleted after each transaction.
     */
    DELETE,
    
    /**
     * Truncate journal mode.
     * <p>
     * Similar to {@code DELETE} but truncates the journal file to zero bytes
     * instead of deleting it, which can be faster on some filesystems.
     */
    TRUNCATE,
    
    /**
     * Persistent journal mode.
     * <p>
     * The journal file is never deleted, only overwritten. Can be slightly
     * faster than {@code DELETE} on filesystems where file creation is expensive.
     */
    PERSIST,
    
    /**
     * Memory journal mode.
     * <p>
     * The journal is kept in memory, not on disk. Transactions are atomic
     * but not durable across application crashes.
     */
    MEMORY,
    
    /**
     * No journal mode.
     * <p>
     * No rollback journal is maintained. Transactions may be rolled back
     * on application crash, leaving the database in an inconsistent state.
     * <strong>Not recommended for production use.</strong>
     */
    OFF
}