package dev.rafex.ether.database.core.sql;

/*-
 * #%L
 * ether-database-core
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
import java.util.Objects;

public record SqlParameter(Object value, Integer sqlType, String arrayElementType) {

    public SqlParameter {
        if (arrayElementType != null && arrayElementType.isBlank()) {
            throw new IllegalArgumentException("arrayElementType must not be blank");
        }
    }

    public static SqlParameter of(final Object value) {
        return new SqlParameter(value, null, null);
    }

    public static SqlParameter nullOf(final int sqlType) {
        return new SqlParameter(null, sqlType, null);
    }

    public static SqlParameter text(final String value) {
        return new SqlParameter(value, Types.VARCHAR, null);
    }

    public static SqlParameter arrayOf(final String arrayElementType, final Object[] values) {
        Objects.requireNonNull(arrayElementType, "arrayElementType");
        return new SqlParameter(values, Types.ARRAY, arrayElementType);
    }
}
