package dev.rafex.ether.database.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;

class PostgresErrorClassifierTest {

	@Test
	void shouldClassifyKnownSqlStates() {
		assertEquals(PostgresErrorClassifier.Category.UNIQUE_VIOLATION,
				PostgresErrorClassifier.classify(new SQLException("duplicate", PostgresSqlStates.UNIQUE_VIOLATION)));
		assertEquals(PostgresErrorClassifier.Category.CONCURRENCY_CONFLICT,
				PostgresErrorClassifier.classify(new SQLException("deadlock", PostgresSqlStates.DEADLOCK_DETECTED)));
	}
}
