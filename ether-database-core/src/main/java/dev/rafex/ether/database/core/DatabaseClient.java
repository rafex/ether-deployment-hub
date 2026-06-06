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

import java.util.List;
import java.util.Optional;

import dev.rafex.ether.database.core.mapping.ResultSetExtractor;
import dev.rafex.ether.database.core.mapping.RowMapper;
import dev.rafex.ether.database.core.sql.SqlQuery;
import dev.rafex.ether.database.core.sql.StatementBinder;
import dev.rafex.ether.database.core.transaction.TransactionRunner;

public interface DatabaseClient extends TransactionRunner {

    <T> T query(SqlQuery query, ResultSetExtractor<T> extractor);

    <T> List<T> queryList(SqlQuery query, RowMapper<T> mapper);

    <T> Optional<T> queryOne(SqlQuery query, RowMapper<T> mapper);

    /**
     * Executes a SQL statement (INSERT, UPDATE, DELETE, or stored procedure call).
     * 
     * @param query the SQL query to execute
     * @return the number of rows affected, or -1 if the statement returned a ResultSet
     */
    int execute(SqlQuery query);

    long[] batch(String sql, List<StatementBinder> binders);
}
