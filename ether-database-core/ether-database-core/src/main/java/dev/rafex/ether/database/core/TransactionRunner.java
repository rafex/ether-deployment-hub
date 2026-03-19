package dev.rafex.ether.database.core;

public interface TransactionRunner {

	<T> T inTransaction(TransactionCallback<T> callback);
}
