package dev.rafex.ether.database.postgres;

public final class PostgresSqlStates {

	public static final String UNIQUE_VIOLATION = "23505";
	public static final String FOREIGN_KEY_VIOLATION = "23503";
	public static final String NOT_NULL_VIOLATION = "23502";
	public static final String CHECK_VIOLATION = "23514";
	public static final String SERIALIZATION_FAILURE = "40001";
	public static final String DEADLOCK_DETECTED = "40P01";

	private PostgresSqlStates() {
	}
}
