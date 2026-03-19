package dev.rafex.ether.database.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
public interface StatementBinder {

	void bind(Connection connection, PreparedStatement statement) throws SQLException;
}
