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
 * SQLite synchronous modes.
 * <p>
 * Controls how aggressively SQLite writes data to disk. Higher durability
 * comes at the cost of performance.
 *
 * @see <a href="https://www.sqlite.org/pragma.html#pragma_synchronous">PRAGMA synchronous</a>
 */
public enum SynchronousMode {

    /**
     * SQLite continues without syncing as soon as it has handed data off to
     * the operating system. Fastest but least safe. Data may be lost in a
     * power loss or operating system crash.
     */
    OFF(0),

    /**
     * SQLite syncs at the most critical moments, but less often than in
     * {@code FULL} mode. A good compromise between safety and speed.
     */
    NORMAL(1),

    /**
     * SQLite syncs the database file after every write transaction.
     * Safest but slowest. Guarantees that data is written to disk.
     */
    FULL(2),

    /**
     * Extra synchronous mode similar to {@code FULL} but with additional
     * directory sync operations for maximum durability.
     */
    EXTRA(3);

    private final int pragmaValue;

    SynchronousMode(int pragmaValue) {
        this.pragmaValue = pragmaValue;
    }

    /**
     * Returns the integer value used in {@code PRAGMA synchronous} statements.
     *
     * @return PRAGMA value
     */
    public int pragmaValue() {
        return pragmaValue;
    }

    /**
     * Returns the SQL string for this synchronous mode.
     *
     * @return SQL string (e.g., "OFF", "NORMAL", "FULL", "EXTRA")
     */
    public String toSql() {
        return name();
    }
}