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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SqlBuilder {

    private final StringBuilder sql;
    private final List<SqlParameter> parameters;

    public SqlBuilder() {
        this("");
    }

    public SqlBuilder(final String initialSql) {
        this.sql = new StringBuilder(Objects.requireNonNull(initialSql, "initialSql"));
        this.parameters = new ArrayList<>();
    }

    public SqlBuilder append(final String fragment) {
        sql.append(fragment);
        return this;
    }

    public SqlBuilder param(final Object value) {
        sql.append('?');
        parameters.add(SqlParameter.of(value));
        return this;
    }

    public SqlBuilder param(final SqlParameter parameter) {
        sql.append('?');
        parameters.add(Objects.requireNonNull(parameter, "parameter"));
        return this;
    }

    public SqlBuilder appendPlaceholders(final int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be >= 0");
        }
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append('?');
        }
        return this;
    }

    public SqlBuilder paramList(final Iterable<?> values) {
        boolean first = true;
        for (final Object value : values) {
            if (!first) {
                sql.append(", ");
            }
            first = false;
            param(value);
        }
        return this;
    }

    public SqlQuery build() {
        return new SqlQuery(sql.toString(), parameters);
    }
}
