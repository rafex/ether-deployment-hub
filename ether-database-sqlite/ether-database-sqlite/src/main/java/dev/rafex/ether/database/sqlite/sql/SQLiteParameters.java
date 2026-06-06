package dev.rafex.ether.database.sqlite.sql;

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

import java.sql.Types;

import dev.rafex.ether.database.core.sql.SqlParameter;

/**
 * SQLite-specific parameter helpers.
 * <p>
 * SQLite has a dynamic type system with five storage classes:
 * <ul>
 *   <li>NULL</li>
 *   <li>INTEGER (signed 64-bit)</li>
 *   <li>REAL (64-bit floating point)</li>
 *   <li>TEXT (UTF-8, UTF-16BE or UTF-16LE)</li>
 *   <li>BLOB (binary data)</li>
 * </ul>
 * JSON support is available via the optional JSON1 extension.
 */
public final class SQLiteParameters {

    private SQLiteParameters() {
    }

    /**
     * Creates a BLOB parameter for binary data.
     *
     * @param data the binary data, may be {@code null}
     * @return a {@code SqlParameter} with type {@link Types#BLOB}
     */
    public static SqlParameter blob(final byte[] data) {
        return data == null ? SqlParameter.nullOf(Types.BLOB) : new SqlParameter(data, Types.BLOB, null);
    }

    /**
     * Creates a JSON parameter.
     * <p>
     * <strong>Note:</strong> Requires the JSON1 extension to be available at runtime.
     * If the extension is not loaded, this will be treated as ordinary TEXT.
     *
     * @param json the JSON string, may be {@code null}
     * @return a {@code SqlParameter} with type {@link Types#OTHER}
     */
    public static SqlParameter json(final String json) {
        return json == null ? SqlParameter.nullOf(Types.OTHER) : new SqlParameter(json, Types.OTHER, null);
    }

    /**
     * Creates an INTEGER parameter.
     *
     * @param value the integer value, may be {@code null}
     * @return a {@code SqlParameter} with type {@link Types#BIGINT}
     */
    public static SqlParameter integer(final Long value) {
        return value == null ? SqlParameter.nullOf(Types.BIGINT) : new SqlParameter(value, Types.BIGINT, null);
    }

    /**
     * Creates a REAL (floating-point) parameter.
     *
     * @param value the floating-point value, may be {@code null}
     * @return a {@code SqlParameter} with type {@link Types#DOUBLE}
     */
    public static SqlParameter real(final Double value) {
        return value == null ? SqlParameter.nullOf(Types.DOUBLE) : new SqlParameter(value, Types.DOUBLE, null);
    }

    /**
     * Creates a TEXT parameter.
     *
     * @param text the text value, may be {@code null}
     * @return a {@code SqlParameter} with type {@link Types#VARCHAR}
     */
    public static SqlParameter text(final String text) {
        return text == null ? SqlParameter.nullOf(Types.VARCHAR) : new SqlParameter(text, Types.VARCHAR, null);
    }
}
