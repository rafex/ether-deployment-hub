# ether-database-sqlite

SQLite-specific extensions for the Ether database stack, built on JDBC and the Java standard library. This module adds three focused utilities on top of `ether-database-core`: error classification by SQLite error codes, SQLite-specific parameter helpers, and Write-Ahead Logging (WAL) configuration support.

## Features

- **SQLite Error Classification**: Map SQLite numeric error codes to typed categories
- **SQLite-specific Parameters**: Helper methods for SQLite data types (BLOB, JSON, INTEGER, REAL, TEXT)
- **Write-Ahead Logging (WAL) Support**: Configuration utilities for SQLite WAL mode and other PRAGMAs
- **SQLite Configuration**: Builder pattern for configuring SQLite connections with optimal settings

## Maven Dependency

```xml
<dependency>
    <groupId>dev.rafex.ether.database</groupId>
    <artifactId>ether-database-sqlite</artifactId>
    <version>9.5.5-SNAPSHOT</version>
</dependency>
```

This module depends on `ether-database-core`. For a complete runnable stack, also add `ether-jdbc` and the SQLite JDBC driver:

```xml
<dependency>
    <groupId>dev.rafex.ether.jdbc</groupId>
    <artifactId>ether-jdbc</artifactId>
    <version>9.5.5-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.45.3.0</version>
    <scope>runtime</scope>
</dependency>
```

---

## Architecture

```mermaid
classDiagram
    direction TB

    namespace ether_database_core {
        class DatabaseClient {
            <<interface>>
        }
        class SqlParameter {
            <<record>>
            +of(Object) SqlParameter
            +nullOf(int) SqlParameter
        }
        class DatabaseAccessException {
            +getCause() Throwable
        }
    }

    namespace ether_jdbc {
        class JdbcDatabaseClient {
            +JdbcDatabaseClient(DataSource)
        }
    }

    namespace ether_database_sqlite {
        class SQLiteErrorCodes {
            <<constants>>
            +UNIQUE_VIOLATION : int = 2067
            +FOREIGN_KEY_VIOLATION : int = 787
            +NOT_NULL_VIOLATION : int = 1299
            +CHECK_VIOLATION : int = 275
            +BUSY : int = 5
            +LOCKED : int = 6
        }

        class SQLiteErrorClassifier {
            +classify(SQLException) Category
        }

        class Category {
            <<enum>>
            UNIQUE_VIOLATION
            FOREIGN_KEY_VIOLATION
            NOT_NULL_VIOLATION
            CHECK_VIOLATION
            BUSY
            LOCKED
            OTHER
        }

        class SQLiteParameters {
            +blob(byte[]) SqlParameter
            +json(String) SqlParameter
            +integer(Long) SqlParameter
            +real(Double) SqlParameter
            +text(String) SqlParameter
        }

        class SQLiteConfig {
            <<builder>>
            +builder() Builder
            +defaults() SQLiteConfig
        }

        class SQLitePragmas {
            +apply(Connection, SQLiteConfig)
            +enableWal(Connection)
            +disableWal(Connection)
        }

        class JournalMode {
            <<enum>>
            WAL
            DELETE
            TRUNCATE
            PERSIST
            MEMORY
            OFF
        }

        class SynchronousMode {
            <<enum>>
            OFF
            NORMAL
            FULL
            EXTRA
        }
    }

    DatabaseClient <|.. JdbcDatabaseClient
    SQLiteErrorClassifier ..> SQLiteErrorCodes : reads
    SQLiteErrorClassifier +-- Category
    SQLiteParameters ..> SqlParameter : creates
    SQLitePragmas ..> SQLiteConfig : uses
    SQLiteConfig +-- JournalMode
    SQLiteConfig +-- SynchronousMode
    DatabaseAccessException ..> SQLiteErrorClassifier : cause inspected by caller
```

---

## Package Layout

| Package | Contents |
|---|---|
| `dev.rafex.ether.database.sqlite.errors` | `SQLiteErrorClassifier`, `SQLiteErrorCodes` |
| `dev.rafex.ether.database.sqlite.sql` | `SQLiteParameters` |
| `dev.rafex.ether.database.sqlite.config` | `SQLiteConfig`, `SQLitePragmas`, `JournalMode`, `SynchronousMode` |

---

## SQLiteErrorCodes

A non-instantiable constants class holding the SQLite error codes most relevant to application error handling. SQLite uses numeric error codes instead of SQL state strings.

| Constant | Error Code | Meaning |
|---|---|---|
| `UNIQUE_VIOLATION` | `2067` | A `UNIQUE` or `PRIMARY KEY` constraint was violated |
| `FOREIGN_KEY_VIOLATION` | `787` | A foreign key reference does not exist |
| `NOT_NULL_VIOLATION` | `1299` | A `NOT NULL` constraint was violated |
| `CHECK_VIOLATION` | `275` | A `CHECK` constraint was violated |
| `BUSY` | `5` | Database is locked (retry with busy timeout) |
| `LOCKED` | `6` | Database table is locked |

---

## SQLiteErrorClassifier

Inspects a `SQLException` and maps its SQLite error code to a typed `Category` enum. SQLite uses `SQLException.getErrorCode()` instead of `getSQLState()`.

| Category | Mapped Error Codes |
|---|---|
| `UNIQUE_VIOLATION` | `2067` |
| `FOREIGN_KEY_VIOLATION` | `787` |
| `NOT_NULL_VIOLATION` | `1299` |
| `CHECK_VIOLATION` | `275` |
| `BUSY` | `5` |
| `LOCKED` | `6` |
| `OTHER` | anything else |

---

## SQLiteParameters

Factory methods that produce `SqlParameter` instances for SQLite-specific column types. SQLite has different type mappings than PostgreSQL.

| Method | SQLite Type | Java Type | Notes |
|---|---|---|---|
| `blob(byte[] data)` | `BLOB` | `byte[]` | Binary data, pass `null` for SQL NULL |
| `json(String json)` | `TEXT` (JSON) | `String` | JSON string, uses `Types.OTHER` |
| `integer(Long value)` | `INTEGER` | `Long` | 64-bit integer, pass `null` for SQL NULL |
| `real(Double value)` | `REAL` | `Double` | Floating point, pass `null` for SQL NULL |
| `text(String value)` | `TEXT` | `String` | Text string, pass `null` for SQL NULL |

---

## SQLite Configuration and WAL Support

SQLite supports Write-Ahead Logging (WAL) mode for better concurrency and performance. This module provides comprehensive configuration utilities.

### JournalMode

Enum representing SQLite journal modes:

| Mode | Description |
|---|---|
| `WAL` | Write-Ahead Logging (recommended for most applications) |
| `DELETE` | Rollback journal (default in SQLite < 3.7.0) |
| `TRUNCATE` | Similar to DELETE but truncates journal file |
| `PERSIST` | Journal file is never deleted, only overwritten |
| `MEMORY` | Journal kept in memory (not durable) |
| `OFF` | No journal (not recommended for production) |

### SynchronousMode

Enum representing SQLite synchronous modes (durability vs performance trade-off):

| Mode | Value | Description |
|---|---|---|
| `OFF` | 0 | Fastest, least safe (may lose data on power loss) |
| `NORMAL` | 1 | Good balance between safety and speed |
| `FULL` | 2 | Safest, slowest (syncs after every transaction) |
| `EXTRA` | 3 | Extra durability with directory syncs |

### SQLiteConfig

Builder pattern for configuring SQLite connections:

```java
SQLiteConfig config = SQLiteConfig.builder()
    .journalMode(JournalMode.WAL)           // Enable WAL
    .synchronousMode(SynchronousMode.NORMAL) // Balance safety/performance
    .foreignKeys(true)                      // Enable foreign key constraints
    .busyTimeout(5000)                      // 5 second busy timeout
    .caseSensitiveLike(false)               // Case-insensitive LIKE
    .recursiveTriggers(false)               // Disable recursive triggers
    .autoVacuum(false)                      // Disable auto-vacuum
    .build();
```

### SQLitePragmas

Utility to apply configuration to SQLite connections:

```java
try (Connection conn = DriverManager.getConnection("jdbc:sqlite:test.db")) {
    SQLitePragmas.apply(conn, config);
    // Connection is now configured with WAL and other optimizations
}
```

Convenience methods:
- `enableWal(Connection)` - Enable WAL mode specifically
- `disableWal(Connection)` - Disable WAL mode
- `setSynchronousMode(Connection, SynchronousMode)` - Set synchronous mode

---

## Example 1 — Classify a DatabaseAccessException at the Service Layer

Catch `DatabaseAccessException` in a service or HTTP handler and convert the underlying SQLite constraint violation into a domain error.

```java
package com.example.users;

import dev.rafex.ether.database.core.DatabaseClient;
import dev.rafex.ether.database.core.exceptions.DatabaseAccessException;
import dev.rafex.ether.database.core.sql.SqlBuilder;
import dev.rafex.ether.database.sqlite.errors.SQLiteErrorClassifier;
import dev.rafex.ether.database.sqlite.errors.SQLiteErrorClassifier.Category;

import java.sql.SQLException;
import java.util.UUID;

public final class UserService {

    private final DatabaseClient db;

    public UserService(DatabaseClient db) {
        this.db = db;
    }

    /**
     * Attempts to insert a user.
     * Returns the new user's ID on success.
     * Throws a domain-specific exception when a unique constraint is violated.
     */
    public UUID createUser(String email, String role) {
        UUID id = UUID.randomUUID();
        var query = new SqlBuilder("INSERT INTO users (id, email, role) VALUES (")
            .param(id)
            .append(", ").param(email)
            .append(", ").param(role)
            .append(")")
            .build();
        try {
            db.execute(query);
            return id;
        } catch (DatabaseAccessException e) {
            // getCause() is always the original SQLException.
            if (e.getCause() instanceof SQLException sqlEx) {
                Category category = SQLiteErrorClassifier.classify(sqlEx);
                if (category == Category.UNIQUE_VIOLATION) {
                    throw new DuplicateEmailException(email, e);
                }
                if (category == Category.FOREIGN_KEY_VIOLATION) {
                    throw new ReferenceNotFoundException("role", role, e);
                }
                if (category == Category.BUSY || category == Category.LOCKED) {
                    throw new DatabaseBusyException("Database is busy, please retry", e);
                }
            }
            throw e; // re-throw unexpected errors
        }
    }

    // Simple domain exceptions
    public static final class DuplicateEmailException extends RuntimeException {
        public DuplicateEmailException(String email, Throwable cause) {
            super("Email already registered: " + email, cause);
        }
    }

    public static final class ReferenceNotFoundException extends RuntimeException {
        public ReferenceNotFoundException(String field, String value, Throwable cause) {
            super("Referenced value not found — " + field + ": " + value, cause);
        }
    }

    public static final class DatabaseBusyException extends RuntimeException {
        public DatabaseBusyException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
```

---

## Example 2 — Use SQLiteParameters with BLOB and JSON

Use `SQLiteParameters.blob()` and `SQLiteParameters.json()` for SQLite-specific data types.

```java
package com.example.documents;

import dev.rafex.ether.database.core.DatabaseClient;
import dev.rafex.ether.database.core.sql.SqlBuilder;
import dev.rafex.ether.database.sqlite.sql.SQLiteParameters;

import java.util.UUID;

public final class DocumentRepository {

    private final DatabaseClient db;

    public DocumentRepository(DatabaseClient db) {
        this.db = db;
    }

    /**
     * Stores a document with binary content and JSON metadata.
     *
     * DDL:
     *   CREATE TABLE documents (
     *     id      UUID PRIMARY KEY,
     *     name    TEXT NOT NULL,
     *     content BLOB,
     *     metadata TEXT  -- JSON stored as TEXT
     *   );
     */
    public int insertDocument(UUID id, String name, byte[] content, String metadataJson) {
        var query = new SqlBuilder("INSERT INTO documents (id, name, content, metadata) VALUES (")
            .param(id)
            .append(", ").param(name)
            .append(", ").param(SQLiteParameters.blob(content))
            .append(", ").param(SQLiteParameters.json(metadataJson))
            .append(")")
            .build();
        return db.execute(query);
    }

    /**
     * Queries documents using JSON functions (requires JSON1 extension).
     */
    public java.util.List<UUID> findByMetadataField(String fieldName, String fieldValue) {
        var query = new SqlBuilder(
                "SELECT id FROM documents WHERE json_extract(metadata, ?) = ?")
            .param("$." + fieldName)
            .append(", ").param(fieldValue)
            .append(" ORDER BY id")
            .build();
        return db.queryList(query, rs -> rs.getObject("id", UUID.class));
    }
}
```

---

## Example 3 — Configure SQLite with WAL and Connection Settings

Configure SQLite connections with Write-Ahead Logging and optimal settings.

```java
package com.example.app;

import dev.rafex.ether.database.sqlite.config.SQLiteConfig;
import dev.rafex.ether.database.sqlite.config.SQLitePragmas;
import dev.rafex.ether.database.sqlite.config.JournalMode;
import dev.rafex.ether.database.sqlite.config.SynchronousMode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseInitializer {

    /**
     * Creates and configures a SQLite connection with optimal settings.
     */
    public static Connection createConnection(String dbPath) throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        
        // Configure with WAL and recommended settings
        SQLiteConfig config = SQLiteConfig.builder()
            .journalMode(JournalMode.WAL)
            .synchronousMode(SynchronousMode.NORMAL)
            .foreignKeys(true)
            .busyTimeout(10000)  // 10 second busy timeout
            .caseSensitiveLike(false)
            .recursiveTriggers(false)
            .autoVacuum(false)
            .build();
        
        SQLitePragmas.apply(conn, config);
        return conn;
    }

    /**
     * Simple convenience method to enable WAL on an existing connection.
     */
    public static void enableWalMode(Connection conn) throws SQLException {
        SQLitePragmas.enableWal(conn);
    }

    /**
     * Set synchronous mode for durability requirements.
     */
    public static void setFullDurability(Connection conn) throws SQLException {
        SQLitePragmas.setSynchronousMode(conn, SynchronousMode.FULL);
    }
}
```

---

## Example 4 — Handle SQLite BUSY and LOCKED Errors with Retry Logic

SQLite returns BUSY (5) and LOCKED (6) error codes when the database is locked. Implement retry logic.

```java
package com.example.orders;

import dev.rafex.ether.database.core.DatabaseClient;
import dev.rafex.ether.database.core.exceptions.DatabaseAccessException;
import dev.rafex.ether.database.core.sql.SqlBuilder;
import dev.rafex.ether.database.sqlite.errors.SQLiteErrorClassifier;
import dev.rafex.ether.database.sqlite.errors.SQLiteErrorClassifier.Category;

import java.sql.SQLException;
import java.util.UUID;

public final class OrderService {

    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY_MS = 100;

    private final DatabaseClient db;

    public OrderService(DatabaseClient db) {
        this.db = db;
    }

    /**
     * Executes an operation with retry logic for SQLite BUSY/LOCKED errors.
     */
    public UUID placeOrder(UUID customerId, UUID productId, int quantity) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return attemptPlaceOrder(customerId, productId, quantity);
            } catch (DatabaseAccessException e) {
                if (e.getCause() instanceof SQLException sqlEx) {
                    Category category = SQLiteErrorClassifier.classify(sqlEx);
                    boolean retryable = category == Category.BUSY || category == Category.LOCKED;
                    
                    if (retryable && attempt < MAX_RETRIES) {
                        // Exponential backoff
                        long delay = RETRY_DELAY_MS * (1L << (attempt - 1));
                        try {
                            Thread.sleep(Math.min(delay, 5000)); // Max 5 seconds
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw e;
                        }
                        continue;
                    }
                }
                throw e;
            }
        }
        throw new IllegalStateException("Unreachable");
    }

    private UUID attemptPlaceOrder(UUID customerId, UUID productId, int quantity) {
        UUID orderId = UUID.randomUUID();
        return db.inTransaction(connection -> {
            // Decrement stock
            try (var ps = connection.prepareStatement(
                    "UPDATE inventory SET stock = stock - ? WHERE product_id = ? AND stock >= ?")) {
                ps.setInt(1, quantity);
                ps.setObject(2, productId);
                ps.setInt(3, quantity);
                if (ps.executeUpdate() == 0) {
                    throw new java.sql.SQLException("Insufficient stock for product: " + productId);
                }
            }
            // Create order
            try (var ps = connection.prepareStatement(
                    "INSERT INTO orders (id, customer_id, product_id, quantity) VALUES (?, ?, ?, ?)")) {
                ps.setObject(1, orderId);
                ps.setObject(2, customerId);
                ps.setObject(3, productId);
                ps.setInt(4, quantity);
                ps.executeUpdate();
            }
            return orderId;
        });
    }
}
```

---

## Example 5 — Full Integration: ether-jdbc + ether-database-sqlite

A complete, runnable setup combining all modules with SQLite WAL configuration.

```java
package com.example;

import dev.rafex.ether.database.core.DatabaseClient;
import dev.rafex.ether.database.core.exceptions.DatabaseAccessException;
import dev.rafex.ether.database.core.sql.SqlBuilder;
import dev.rafex.ether.database.sqlite.config.SQLiteConfig;
import dev.rafex.ether.database.sqlite.config.SQLitePragmas;
import dev.rafex.ether.database.sqlite.config.JournalMode;
import dev.rafex.ether.database.sqlite.config.SynchronousMode;
import dev.rafex.ether.database.sqlite.errors.SQLiteErrorClassifier;
import dev.rafex.ether.database.sqlite.errors.SQLiteErrorClassifier.Category;
import dev.rafex.ether.database.sqlite.sql.SQLiteParameters;
import dev.rafex.ether.jdbc.client.JdbcDatabaseClient;
import dev.rafex.ether.jdbc.datasource.SimpleDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Self-contained example that writes and reads a "product" table
 * with SQLite-specific features including WAL configuration.
 *
 * DDL:
 *   CREATE TABLE products (
 *     id       UUID PRIMARY KEY,
 *     sku      TEXT UNIQUE NOT NULL,
 *     metadata TEXT,  -- JSON stored as TEXT
 *     image    BLOB,
 *     price    REAL
 *   );
 */
public final class ProductDemo {

    record Product(UUID id, String sku, String metadataJson, byte[] image, double price) {}

    private final DatabaseClient db;

    public ProductDemo(DatabaseClient db) {
        this.db = db;
    }

    public UUID createProduct(Product product) {
        var query = new SqlBuilder("INSERT INTO products (id, sku, metadata, image, price) VALUES (")
            .param(product.id())
            .append(", ").param(product.sku())
            .append(", ").param(SQLiteParameters.json(product.metadataJson()))
            .append(", ").param(SQLiteParameters.blob(product.image()))
            .append(", ").param(SQLiteParameters.real(product.price()))
            .append(")")
            .build();
        try {
            db.execute(query);
            return product.id();
        } catch (DatabaseAccessException e) {
            if (e.getCause() instanceof SQLException sqlEx) {
                Category cat = SQLiteErrorClassifier.classify(sqlEx);
                if (cat == Category.UNIQUE_VIOLATION) {
                    throw new IllegalArgumentException("SKU already exists: " + product.sku(), e);
                }
            }
            throw e;
        }
    }

    public List<UUID> findByPriceRange(double minPrice, double maxPrice) {
        var query = new SqlBuilder(
                "SELECT id FROM products WHERE price BETWEEN ? AND ?")
            .param(SQLiteParameters.real(minPrice))
            .append(" AND ").param(SQLiteParameters.real(maxPrice))
            .append(" ORDER BY price")
            .build();
        return db.queryList(query, rs -> rs.getObject("id", UUID.class));
    }

    public static void main(String[] args) throws SQLException {
        // Create data source
        SimpleDataSource dataSource = new SimpleDataSource("jdbc:sqlite:products.db", null, null);
        
        // Configure connection with WAL
        try (Connection conn = dataSource.getConnection()) {
            SQLiteConfig config = SQLiteConfig.builder()
                .journalMode(JournalMode.WAL)
                .synchronousMode(SynchronousMode.NORMAL)
                .foreignKeys(true)
                .busyTimeout(5000)
                .build();
            SQLitePragmas.apply(conn, config);
        }
        
        // Create database client
        DatabaseClient db = new JdbcDatabaseClient(dataSource);
        var demo = new ProductDemo(db);

        // Create table (simplified)
        db.execute("CREATE TABLE IF NOT EXISTS products (" +
                   "id UUID PRIMARY KEY, sku TEXT UNIQUE NOT NULL, " +
                   "metadata TEXT, image BLOB, price REAL)");

        UUID id = demo.createProduct(new Product(
            UUID.randomUUID(),
            "SKU-001",
            "{\"weight\": 1.5, \"unit\": \"kg\"}",
            new byte[]{1, 2, 3, 4, 5},  // Sample image data
            29.99
        ));
        System.out.println("Created product: " + id);

        List<UUID> found = demo.findByPriceRange(20.0, 40.0);
        System.out.println("Products in price range: " + found);
    }
}
```

---

## SQLite-Specific Considerations

### Error Codes vs SQL States
- SQLite uses numeric error codes (`SQLException.getErrorCode()`)
- PostgreSQL uses 5-character SQL states (`SQLException.getSQLState()`)
- `SQLiteErrorClassifier` works with error codes, not SQL states

### Type System
- SQLite has dynamic typing (affinity system)
- Use `SQLiteParameters` for proper type mapping to JDBC
- JSON is stored as TEXT with optional JSON1 extension

### Concurrency
- WAL mode allows concurrent reads and writes
- Use busy timeout (`PRAGMA busy_timeout`) for automatic retry
- Implement application-level retry for BUSY/LOCKED errors

### Configuration
- Configure connections immediately after opening
- WAL mode persists across connections to same database
- Consider synchronous mode based on durability requirements

---

## Scope

- SQLite error code classification helpers
- SQLite parameter helpers for BLOB, JSON, INTEGER, REAL, TEXT
- Write-Ahead Logging (WAL) configuration utilities
- Thin utilities that complement `ether-database-core` and `ether-jdbc`

## Notes

- This module is still JDBC-based; it does not introduce a SQLite framework or ORM.
- It is the correct place for SQLite-specific helpers such as WAL configuration and SQLite type handling.
- `SQLiteErrorClassifier` operates on `SQLException` instances, not on `DatabaseAccessException`. Callers must unwrap `DatabaseAccessException.getCause()` before classifying.
- Licensed under MIT. Source: https://github.com/rafex/ether-database-sqlite