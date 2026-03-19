package dev.rafex.ether.database.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record SqlQuery(String sql, List<SqlParameter> parameters) {

	public SqlQuery {
		Objects.requireNonNull(sql, "sql");
		Objects.requireNonNull(parameters, "parameters");
		parameters = List.copyOf(new ArrayList<>(parameters));
	}

	public static SqlQuery of(final String sql) {
		return new SqlQuery(sql, List.of());
	}
}
