package dev.rafex.ether.database.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Types;
import java.util.List;

import org.junit.jupiter.api.Test;

class SqlBuilderTest {

	@Test
	void shouldBuildSqlWithParameters() {
		final var query = new SqlBuilder("select * from users where status = ")
				.param("active")
				.append(" and role in (")
				.paramList(List.of("admin", "user"))
				.append(")")
				.build();

		assertEquals("select * from users where status = ? and role in (?, ?)", query.sql());
		assertEquals(3, query.parameters().size());
		assertEquals("active", query.parameters().get(0).value());
	}

	@Test
	void shouldSupportNullAndArrayParameters() {
		final var query = new SqlQuery("select * from events where archived_at is ? and tags && ?",
				List.of(SqlParameter.nullOf(Types.TIMESTAMP), SqlParameter.arrayOf("text", new String[] { "a", "b" })));

		assertEquals(2, query.parameters().size());
		assertEquals("text", query.parameters().get(1).arrayElementType());
	}
}
