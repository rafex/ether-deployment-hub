package dev.rafex.ether.database.core;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

public final class ResultSets {

	private ResultSets() {
	}

	public static Instant getInstant(final ResultSet resultSet, final String column) throws SQLException {
		final var timestamp = resultSet.getTimestamp(column);
		return timestamp == null ? null : timestamp.toInstant();
	}

	public static UUID getUuid(final ResultSet resultSet, final String column) throws SQLException {
		return resultSet.getObject(column, UUID.class);
	}

	public static String[] getStringArray(final ResultSet resultSet, final String column) throws SQLException {
		final var array = resultSet.getArray(column);
		return array == null ? null : (String[]) array.getArray();
	}
}
