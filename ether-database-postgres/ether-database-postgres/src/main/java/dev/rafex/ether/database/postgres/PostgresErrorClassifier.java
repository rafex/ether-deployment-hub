package dev.rafex.ether.database.postgres;

import java.sql.SQLException;
import java.util.Objects;

public final class PostgresErrorClassifier {

	public enum Category {
		UNIQUE_VIOLATION,
		FOREIGN_KEY_VIOLATION,
		NOT_NULL_VIOLATION,
		CHECK_VIOLATION,
		CONCURRENCY_CONFLICT,
		OTHER
	}

	private PostgresErrorClassifier() {
	}

	public static Category classify(final SQLException exception) {
		Objects.requireNonNull(exception, "exception");
		final String state = exception.getSQLState();
		if (PostgresSqlStates.UNIQUE_VIOLATION.equals(state)) {
			return Category.UNIQUE_VIOLATION;
		}
		if (PostgresSqlStates.FOREIGN_KEY_VIOLATION.equals(state)) {
			return Category.FOREIGN_KEY_VIOLATION;
		}
		if (PostgresSqlStates.NOT_NULL_VIOLATION.equals(state)) {
			return Category.NOT_NULL_VIOLATION;
		}
		if (PostgresSqlStates.CHECK_VIOLATION.equals(state)) {
			return Category.CHECK_VIOLATION;
		}
		if (PostgresSqlStates.SERIALIZATION_FAILURE.equals(state)
				|| PostgresSqlStates.DEADLOCK_DETECTED.equals(state)) {
			return Category.CONCURRENCY_CONFLICT;
		}
		return Category.OTHER;
	}
}
