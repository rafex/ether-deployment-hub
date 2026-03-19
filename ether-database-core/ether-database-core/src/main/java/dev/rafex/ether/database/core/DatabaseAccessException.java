package dev.rafex.ether.database.core;

public class DatabaseAccessException extends RuntimeException {

	public DatabaseAccessException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
