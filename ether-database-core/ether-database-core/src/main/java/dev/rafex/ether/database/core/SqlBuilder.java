package dev.rafex.ether.database.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SqlBuilder {

	private final StringBuilder sql;
	private final List<SqlParameter> parameters;

	public SqlBuilder() {
		this("");
	}

	public SqlBuilder(final String initialSql) {
		this.sql = new StringBuilder(Objects.requireNonNull(initialSql, "initialSql"));
		this.parameters = new ArrayList<>();
	}

	public SqlBuilder append(final String fragment) {
		sql.append(fragment);
		return this;
	}

	public SqlBuilder param(final Object value) {
		sql.append('?');
		parameters.add(SqlParameter.of(value));
		return this;
	}

	public SqlBuilder param(final SqlParameter parameter) {
		sql.append('?');
		parameters.add(Objects.requireNonNull(parameter, "parameter"));
		return this;
	}

	public SqlBuilder appendPlaceholders(final int count) {
		if (count < 0) {
			throw new IllegalArgumentException("count must be >= 0");
		}
		for (int i = 0; i < count; i++) {
			if (i > 0) {
				sql.append(", ");
			}
			sql.append('?');
		}
		return this;
	}

	public SqlBuilder paramList(final Iterable<?> values) {
		boolean first = true;
		for (final Object value : values) {
			if (!first) {
				sql.append(", ");
			}
			first = false;
			param(value);
		}
		return this;
	}

	public SqlQuery build() {
		return new SqlQuery(sql.toString(), parameters);
	}
}
