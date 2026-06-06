package dev.rafex.ether.database.core;

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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Types;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.rafex.ether.database.core.sql.SqlBuilder;
import dev.rafex.ether.database.core.sql.SqlParameter;
import dev.rafex.ether.database.core.sql.SqlQuery;

class SqlBuilderTest {

    @Test
    void shouldBuildSqlWithParameters() {
        final var query = new SqlBuilder("select * from users where status = ").param("active").append(" and role in (")
                .paramList(List.of("admin", "user")).append(")").build();

        assertEquals("select * from users where status = ? and role in (?, ?)", query.sql());
        assertEquals(3, query.parameters().size());
        assertEquals("active", query.parameters().get(0).value());
    }

    @Test
    void shouldSupportNullAndArrayParameters() {
        final var query = new SqlQuery("select * from events where archived_at is ? and tags && ?",
                List.of(SqlParameter.nullOf(Types.TIMESTAMP), SqlParameter.arrayOf("text", new String[] { "a", "b" })));

        assertEquals(2, query.parameters().size());
        assertEquals("text", query.parameters().get(1).arrayElementType());
    }
}
