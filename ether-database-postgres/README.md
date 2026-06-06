# ether-database-postgres

PostgreSQL-specific extensions for the Ether database stack, built on JDBC and the Java standard library. This module adds three focused utilities on top of `ether-database-core`: error classification by SQL state, SQL state constants for direct comparison, and parameter helpers for PostgreSQL-native types such as `jsonb`, `text[]`, and `uuid[]`.

## Maven Dependency

```xml
<dependency>
    <groupId>dev.rafex.ether.database</groupId>
    <artifactId>ether-database-postgres</artifactId>
    <version>8.0.0-SNAPSHOT</version>
</dependency>
```

This module depends on `ether-database-core`. For a complete runnable stack, also add `ether-jdbc` and the PostgreSQL JDBC driver:

```xml
<dependency>
    <groupId>dev.rafex.ether.jdbc</groupId>
    <artifactId>ether-jdbc</artifactId>
    <version>8.0.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.3</version>
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
            +arrayOf(String, Object[]) SqlParameter
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

    namespace ether_database_postgres {
        class PostgresSqlStates {
            <<constants>>
            +UNIQUE_VIOLATION : String = "23505"
            +FOREIGN_KEY_VIOLATION : String = "23503"
            +NOT_NULL_VIOLATION : String = "23502"
            +CHECK_VIOLATION : String = "23514"
            +SERIALIZATION_FAILURE : String = "40001"
            +DEADLOCK_DETECTED : String = "40P01"
        }

        class PostgresErrorClassifier {
            +classify(SQLException) Category
        }

        class Category {
            <<enum>>
            UNIQUE_VIOLATION
            FOREIGN_KEY_VIOLATION
            NOT_NULL_VIOLATION
            CHECK_VIOLATION
            CONCURRENCY_CONFLICT
            OTHER
        }

        class PostgresParameters {
            +jsonb(String) SqlParameter
            +textArray(String...) SqlParameter
            +uuidArray(UUID...) SqlParameter
        }
    }

    DatabaseClient <|.. JdbcDatabaseClient
    PostgresErrorClassifier ..> PostgresSqlStates : reads
    PostgresErrorClassifier +-- Category
    PostgresParameters ..> SqlParameter : creates
    DatabaseAccessException ..> PostgresErrorClassifier : cause inspected by caller
```

---

## Package Layout

| Package | Contents |
|---|---|
| `dev.rafex.ether.database.postgres.errors` | `PostgresErrorClassifier`, `PostgresSqlStates` |
| `dev.rafex.ether.database.postgres.sql` | `PostgresParameters` |

---

## PostgresSqlStates

A non-instantiable constants class holding the PostgreSQL SQL state codes most relevant to application error handling. These are the five-character codes returned by `SQLException.getSQLState()`.

| Constant | SQL State | Meaning |
|---|---|---|
| `UNIQUE_VIOLATION` | `23505` | A `UNIQUE` or `PRIMARY KEY` constraint was violated |
| `FOREIGN_KEY_VIOLATION` | `23503` | A foreign key reference does not exist |
| `NOT_NULL_VIOLATION` | `23502` | A `NOT NULL` constraint was violated |
| `CHECK_VIOLATION` | `23514` | A `CHECK` constraint was violated |
| `SERIALIZATION_FAILURE` | `40001` | Transaction could not be serialized (retry required) |
| `DEADLOCK_DETECTED` | `40P01` | PostgreSQL detected a deadlock |

---

## PostgresErrorClassifier

Inspects a `SQLException` and maps its SQL state to a typed `Category` enum. This is the recommended way to branch on constraint violations in service or handler code without string-matching SQL state codes manually.

| Category | Mapped SQL States |
|---|---|
| `UNIQUE_VIOLATION` | `23505` |
| `FOREIGN_KEY_VIOLATION` | `23503` |
| `NOT_NULL_VIOLATION` | `23502` |
| `CHECK_VIOLATION` | `23514` |
| `CONCURRENCY_CONFLICT` | `40001`, `40P01` |
| `OTHER` | anything else |

---

## PostgresParameters

Factory methods that produce `SqlParameter` instances for PostgreSQL-specific column types. The underlying `SqlParameter` records from `ether-database-core` carry the SQL type metadata; `JdbcDatabaseClient` uses that metadata during binding to call `setObject(index, value, Types.OTHER)` for `jsonb` and `connection.createArrayOf("text", values)` for arrays.

| Method | PostgreSQL type | Notes |
|---|---|---|
| `jsonb(String json)` | `jsonb` | Pass `null` to produce a typed SQL NULL |
| `textArray(String... values)` | `text[]` | Varargs — pass individual strings |
| `uuidArray(UUID... values)` | `uuid[]` | Varargs — pass individual UUIDs |

---

## Example 1 — Classify a DatabaseAccessException at the Service Layer

The most common use: catch `DatabaseAccessException` in a service or HTTP handler and convert the underlying PostgreSQL constraint violation into a domain error or HTTP status code.

```java
package com.example.users;

import dev.rafex.ether.database.core.DatabaseClient;
import dev.rafex.ether.database.core.exceptions.DatabaseAccessException;
import dev.rafex.ether.database.core.sql.SqlBuilder;
import dev.rafex.ether.database.postgres.errors.PostgresErrorClassifier;
import dev.rafex.ether.database.postgres.errors.PostgresErrorClassifier.Category;

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
     * Throws a domain-specific exception when a unique constraint is violated
     * (e.g., the email column has a UNIQUE index).
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
                Category category = PostgresErrorClassifier.classify(sqlEx);
                if (category == Category.UNIQUE_VIOLATION) {
                    throw new DuplicateEmailException(email, e);
                }
                if (category == Category.FOREIGN_KEY_VIOLATION) {
                    throw new ReferenceNotFoundException("role", role, e);
                }
            }
            throw e; // re-throw unexpected errors
        }
    }

    // Simple domain exceptions — replace with your own hierarchy.
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
}
```

---

## Example 2 — Use PostgresSqlStates Directly for Specific Checks

When you need a single targeted check (e.g., only care about serialization failures for retry logic), compare the SQL state directly using the constants:

```java
package com.example.orders;

import dev.rafex.ether.database.core.DatabaseClient;
import dev.rafex.ether.database.core.exceptions.DatabaseAccessException;
import dev.rafex.ether.database.core.sql.SqlBuilder;
import dev.rafex.ether.database.postgres.errors.PostgresSqlStates;

import java.sql.SQLException;
import java.util.UUID;

public final class OrderService {

    private static final int MAX_RETRIES = 3;

    private final DatabaseClient db;

    public OrderService(DatabaseClient db) {
        this.db = db;
    }

    /**
     * Executes a serializable transaction and retries up to MAX_RETRIES times
     * if PostgreSQL reports a serialization failure (40001) or deadlock (40P01).
     */
    public UUID placeOrder(UUID customerId, UUID productId, int quantity) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return attemptPlaceOrder(customerId, productId, quantity);
            } catch (DatabaseAccessException e) {
                if (e.getCause() instanceof SQLException sqlEx) {
                    String state = sqlEx.getSQLState();
                    boolean retryable =
                        PostgresSqlStates.SERIALIZATION_FAILURE.equals(state) ||
                        PostgresSqlStates.DEADLOCK_DETECTED.equals(state);
                    if (retryable && attempt < MAX_RETRIES) {
                        // Brief back-off before retrying
                        try { Thread.sleep(50L * attempt); } catch (InterruptedException ie) {
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

## Example 3 — PostgresParameters with jsonb

Use `PostgresParameters.jsonb` to pass JSON strings to `jsonb` columns. `JdbcDatabaseClient` binds these with `Types.OTHER`, which is what the PostgreSQL JDBC driver requires for `jsonb`.

```java
package com.example.events;

import dev.rafex.ether.database.core.DatabaseClient;
import dev.rafex.ether.database.core.sql.SqlBuilder;
import dev.rafex.ether.database.postgres.sql.PostgresParameters;

import java.util.UUID;

public final class EventRepository {

    private final DatabaseClient db;

    public EventRepository(DatabaseClient db) {
        this.db = db;
    }

    /**
     * Stores a domain event with a JSON payload in a jsonb column.
     *
     * DDL:
     *   CREATE TABLE events (
     *     id      UUID PRIMARY KEY,
     *     type    TEXT NOT NULL,
     *     payload JSONB NOT NULL
     *   );
     */
    public int insertEvent(UUID id, String type, String payloadJson) {
        var query = new SqlBuilder("INSERT INTO events (id, type, payload) VALUES (")
            .param(id)
            .append(", ").param(type)
            .append(", ").param(PostgresParameters.jsonb(payloadJson))
            .append(")")
            .build();
        return db.execute(query);
    }

    /**
     * Queries events using a jsonb containment operator (@>).
     * The parameter is still a jsonb value — PostgresParameters.jsonb wraps it correctly.
     */
    public java.util.List<UUID> findByPayloadField(String filterJson) {
        var query = new SqlBuilder(
                "SELECT id FROM events WHERE payload @> ")
            .param(PostgresParameters.jsonb(filterJson))
            .append("::jsonb ORDER BY id")
            .build();
        return db.queryList(query, rs -> rs.getObject("id", UUID.class));
    }

    /**
     * Sets the payload to SQL NULL (typed as jsonb) when archiving.
     */
    public int clearPayload(UUID id) {
        var query = new SqlBuilder("UPDATE events SET payload = ")
            .param(PostgresParameters.jsonb(null))  // typed NULL
            .append(" WHERE id = ").param(id)
            .build();
        return db.execute(query);
    }
}
```

---

## Example 4 — PostgresParameters with Text and UUID Arrays

Use `textArray` and `uuidArray` to pass values to `text[]` and `uuid[]` columns, or to use PostgreSQL array operators such as `&&` (overlaps) and `@>` (contains).

```java
package com.example.articles;

import dev.rafex.ether.database.core.DatabaseClient;
import dev.rafex.ether.database.core.sql.SqlBuilder;
import dev.rafex.ether.database.postgres.sql.PostgresParameters;

import java.util.List;
import java.util.UUID;

public final class ArticleRepository {

    private final DatabaseClient db;

    public ArticleRepository(DatabaseClient db) {
        this.db = db;
    }

    /**
     * Stores an article with a text[] tags column.
     *
     * DDL:
     *   CREATE TABLE articles (
     *     id    UUID PRIMARY KEY,
     *     title TEXT NOT NULL,
     *     tags  TEXT[]
     *   );
     */
    public int insertArticle(UUID id, String title, String... tags) {
        var query = new SqlBuilder("INSERT INTO articles (id, title, tags) VALUES (")
            .param(id)
            .append(", ").param(title)
            .append(", ").param(PostgresParameters.textArray(tags))
            .append(")")
            .build();
        return db.execute(query);
    }

    /**
     * Finds articles whose tags overlap with any of the supplied tags.
     * Uses the PostgreSQL && (overlap) array operator.
     */
    public List<String> findTitlesByTags(String... searchTags) {
        var query = new SqlBuilder(
                "SELECT title FROM articles WHERE tags && ")
            .param(PostgresParameters.textArray(searchTags))
            .append(" ORDER BY title")
            .build();
        return db.queryList(query, rs -> rs.getString("title"));
    }

    /**
     * Finds articles by a list of UUIDs using the PostgreSQL uuid[] type.
     * This is more efficient than an IN clause for large ID lists.
     *
     * DDL:
     *   CREATE TABLE article_permissions (
     *     article_id UUID REFERENCES articles(id),
     *     allowed_user_ids UUID[]
     *   );
     */
    public List<UUID> findArticlesAccessibleByUsers(UUID... userIds) {
        var query = new SqlBuilder(
                "SELECT article_id FROM article_permissions WHERE allowed_user_ids && ")
            .param(PostgresParameters.uuidArray(userIds))
            .build();
        return db.queryList(query, rs -> rs.getObject("article_id", UUID.class));
    }
}
```

---

## Example 5 — Full Integration: ether-jdbc + ether-database-postgres

A complete, runnable setup combining all three modules:

```java
package com.example;

import dev.rafex.ether.database.core.DatabaseClient;
import dev.rafex.ether.database.core.exceptions.DatabaseAccessException;
import dev.rafex.ether.database.core.sql.SqlBuilder;
import dev.rafex.ether.database.postgres.errors.PostgresErrorClassifier;
import dev.rafex.ether.database.postgres.errors.PostgresErrorClassifier.Category;
import dev.rafex.ether.database.postgres.sql.PostgresParameters;
import dev.rafex.ether.jdbc.client.JdbcDatabaseClient;
import dev.rafex.ether.jdbc.datasource.SimpleDataSource;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Self-contained example that writes and reads a "product" table
 * with a jsonb metadata column and a text[] tags column.
 *
 * DDL:
 *   CREATE TABLE products (
 *     id       UUID PRIMARY KEY,
 *     sku      TEXT UNIQUE NOT NULL,
 *     metadata JSONB,
 *     tags     TEXT[]
 *   );
 */
public final class ProductDemo {

    record Product(UUID id, String sku, String metadataJson, String[] tags) {}

    private final DatabaseClient db;

    public ProductDemo(DatabaseClient db) {
        this.db = db;
    }

    public UUID createProduct(Product product) {
        var query = new SqlBuilder("INSERT INTO products (id, sku, metadata, tags) VALUES (")
            .param(product.id())
            .append(", ").param(product.sku())
            .append(", ").param(PostgresParameters.jsonb(product.metadataJson()))
            .append(", ").param(PostgresParameters.textArray(product.tags()))
            .append(")")
            .build();
        try {
            db.execute(query);
            return product.id();
        } catch (DatabaseAccessException e) {
            if (e.getCause() instanceof SQLException sqlEx) {
                Category cat = PostgresErrorClassifier.classify(sqlEx);
                if (cat == Category.UNIQUE_VIOLATION) {
                    throw new IllegalArgumentException("SKU already exists: " + product.sku(), e);
                }
            }
            throw e;
        }
    }

    public List<UUID> findByTags(String... tags) {
        var query = new SqlBuilder(
                "SELECT id FROM products WHERE tags && ")
            .param(PostgresParameters.textArray(tags))
            .append(" ORDER BY id")
            .build();
        return db.queryList(query, rs -> rs.getObject("id", UUID.class));
    }

    public static void main(String[] args) {
        DatabaseClient db = new JdbcDatabaseClient(
            new SimpleDataSource("jdbc:postgresql://localhost:5432/mydb", "app", "secret")
        );
        var demo = new ProductDemo(db);

        UUID id = demo.createProduct(new Product(
            UUID.randomUUID(),
            "SKU-001",
            "{\"weight\": 1.5, \"unit\": \"kg\"}",
            new String[]{"hardware", "tools"}
        ));
        System.out.println("Created product: " + id);

        List<UUID> found = demo.findByTags("tools");
        System.out.println("Products tagged 'tools': " + found);
    }
}
```

---

## Scope

- SQLState classification helpers
- PostgreSQL parameter helpers for `jsonb`, `text[]`, and `uuid[]`
- Thin utilities that complement `ether-database-core` and `ether-jdbc`

## Notes

- This module is still JDBC-based; it does not introduce a PostgreSQL framework or ORM.
- It is the correct place for vendor-specific helpers such as `RETURNING`, `jsonb`, and array handling.
- `PostgresErrorClassifier` operates on `SQLException` instances, not on `DatabaseAccessException`. Callers must unwrap `DatabaseAccessException.getCause()` before classifying.
- Licensed under MIT. Source: https://github.com/rafex/ether-database-postgres
