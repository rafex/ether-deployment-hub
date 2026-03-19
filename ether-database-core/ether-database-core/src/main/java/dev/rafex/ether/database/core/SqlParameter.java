package dev.rafex.ether.database.core;

import java.sql.Types;
import java.util.Objects;

public record SqlParameter(Object value, Integer sqlType, String arrayElementType) {

	public SqlParameter {
		if (arrayElementType != null && arrayElementType.isBlank()) {
			throw new IllegalArgumentException("arrayElementType must not be blank");
		}
	}

	public static SqlParameter of(final Object value) {
		return new SqlParameter(value, null, null);
	}

	public static SqlParameter nullOf(final int sqlType) {
		return new SqlParameter(null, sqlType, null);
	}

	public static SqlParameter text(final String value) {
		return new SqlParameter(value, Types.VARCHAR, null);
	}

	public static SqlParameter arrayOf(final String arrayElementType, final Object[] values) {
		Objects.requireNonNull(arrayElementType, "arrayElementType");
		return new SqlParameter(values, Types.ARRAY, arrayElementType);
	}
}
