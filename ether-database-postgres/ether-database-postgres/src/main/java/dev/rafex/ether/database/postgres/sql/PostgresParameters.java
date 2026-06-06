package dev.rafex.ether.database.postgres.sql;

/*-
 * #%L
 * ether-database-postgres
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
import java.util.UUID;

import dev.rafex.ether.database.core.sql.SqlParameter;

public final class PostgresParameters {

    private PostgresParameters() {
    }

    public static SqlParameter jsonb(final String json) {
        return json == null ? SqlParameter.nullOf(Types.OTHER) : new SqlParameter(json, Types.OTHER, null);
    }

    public static SqlParameter textArray(final String... values) {
        return SqlParameter.arrayOf("text", values);
    }

    public static SqlParameter uuidArray(final UUID... values) {
        return SqlParameter.arrayOf("uuid", values);
    }
}
