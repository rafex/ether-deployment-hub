package dev.rafex.ether.database.core;

import java.util.List;
import java.util.Optional;

public interface DatabaseClient extends TransactionRunner {

	<T> T query(SqlQuery query, ResultSetExtractor<T> extractor);

	<T> List<T> queryList(SqlQuery query, RowMapper<T> mapper);

	<T> Optional<T> queryOne(SqlQuery query, RowMapper<T> mapper);

	int execute(SqlQuery query);

	long[] batch(String sql, List<StatementBinder> binders);
}
