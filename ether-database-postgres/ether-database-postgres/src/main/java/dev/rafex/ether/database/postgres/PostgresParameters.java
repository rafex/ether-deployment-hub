package dev.rafex.ether.database.postgres;

import java.sql.Types;
import java.util.UUID;

import dev.rafex.ether.database.core.SqlParameter;

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
