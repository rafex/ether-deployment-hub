package dev.rafex.ether.database.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Types;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PostgresParametersTest {

	@Test
	void shouldCreateJsonbAndArrayParameters() {
		assertEquals(Types.OTHER, PostgresParameters.jsonb("{}").sqlType());
		assertEquals("text", PostgresParameters.textArray("a", "b").arrayElementType());
		assertEquals("uuid", PostgresParameters.uuidArray(UUID.randomUUID()).arrayElementType());
	}
}
