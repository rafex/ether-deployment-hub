package dev.rafex.ether.database.sqlite.errors;

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

import java.sql.SQLException;
import java.util.Objects;

import dev.rafex.ether.database.core.exceptions.DatabaseAccessException;

/**
 * Classifies SQLite errors.
 * <p>
 * Currently returns {@link DatabaseAccessException} for all SQLite errors.
 * In the future, this could be extended to return more specific exception types.
 */
public final class SQLiteErrorClassifier {

    private SQLiteErrorClassifier() {
    }

    /**
     * Classifies a SQLite exception.
     *
     * @param exception the SQL exception to classify
     * @return a runtime exception (currently always {@link DatabaseAccessException})
     * @throws NullPointerException if {@code exception} is {@code null}
     */
    public static DatabaseAccessException classify(final SQLException exception) {
        Objects.requireNonNull(exception, "exception");
        final String message = exception.getMessage();
        return new DatabaseAccessException(message, exception);
    }

    /**
     * Classifies a SQLite exception with a custom message.
     *
     * @param message the custom error message
     * @param exception the SQL exception to classify
     * @return a runtime exception (currently always {@link DatabaseAccessException})
     * @throws NullPointerException if {@code exception} is {@code null}
     */
    public static DatabaseAccessException classify(final String message, final SQLException exception) {
        Objects.requireNonNull(exception, "exception");
        return new DatabaseAccessException(message, exception);
    }
}