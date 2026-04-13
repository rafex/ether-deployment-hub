# Guía práctica: ether-database-core

**ether-database-core** define los contratos para acceso a base de datos: `DatabaseClient`,
`RowMapper`, `ResultSetExtractor`, `SqlQuery` y gestión de transacciones.
Los adapters concretos (`ether-jdbc`) implementan estas interfaces.

## Instalación

```xml
<dependency>
    <groupId>dev.rafex.ether.database.core</groupId>
    <artifactId>ether-database-core</artifactId>
    <version>8.0.0-SNAPSHOT</version>
</dependency>
```

---

## `SqlQuery` — construcción de consultas

```java
// Sin parámetros
SqlQuery q1 = SqlQuery.of("SELECT * FROM users");

// Con parámetros posicionales
SqlQuery q2 = SqlQuery.of(
    "SELECT * FROM users WHERE active = ? AND role = ?",
    List.of(SqlParameter.of(true), SqlParameter.of("admin"))
);
```

---

## `DatabaseClient` — operaciones de lectura

```java
DatabaseClient db = /* inyectado */;

// Consulta que devuelve lista
List<User> users = db.queryList(
    SqlQuery.of("SELECT id, name, email FROM users WHERE active = ?",
        List.of(SqlParameter.of(true))),
    rs -> new User(
        rs.getLong("id"),
        rs.getString("name"),
        rs.getString("email")
    )
);

// Consulta que devuelve un solo registro
Optional<User> user = db.queryOne(
    SqlQuery.of("SELECT id, name, email FROM users WHERE id = ?",
        List.of(SqlParameter.of(userId))),
    rs -> new User(rs.getLong("id"), rs.getString("name"), rs.getString("email"))
);

// Extracción personalizada del ResultSet completo
int total = db.query(
    SqlQuery.of("SELECT COUNT(*) FROM users"),
    rs -> { rs.next(); return rs.getInt(1); }
);
```

---

## `DatabaseClient` — operaciones de escritura

```java
// INSERT / UPDATE / DELETE — devuelve filas afectadas
int rows = db.execute(
    SqlQuery.of("UPDATE users SET active = ? WHERE id = ?",
        List.of(SqlParameter.of(false), SqlParameter.of(userId)))
);

// Batch — devuelve array con filas afectadas por cada operación
long[] results = db.batch(
    "INSERT INTO events (type, payload) VALUES (?, ?)",
    List.of(
        stmt -> { stmt.setString(1, "LOGIN"); stmt.setString(2, "{...}"); },
        stmt -> { stmt.setString(1, "LOGOUT"); stmt.setString(2, "{...}"); }
    )
);
```

---

## Transacciones

```java
User created = db.inTransaction(tx -> {
    tx.execute(SqlQuery.of("INSERT INTO users (name, email) VALUES (?, ?)",
        List.of(SqlParameter.of("Alice"), SqlParameter.of("alice@example.com"))));
    long id = tx.queryOne(
        SqlQuery.of("SELECT lastval()"), rs -> rs.getLong(1)
    ).orElseThrow();
    tx.execute(SqlQuery.of("INSERT INTO roles (user_id, role) VALUES (?, ?)",
        List.of(SqlParameter.of(id), SqlParameter.of("USER"))));
    return new User(id, "Alice", "alice@example.com");
});
```

---

## `DatabaseAccessException`

```java
try {
    db.execute(insertQuery);
} catch (DatabaseAccessException e) {
    // e.getCause() — SQLException original
    // usar ether-database-postgres para clasificar el error
}
```

---

## Integración con ether-di

```java
public class AppContainer {

    private final Lazy<DatabaseClient> db = new Lazy<>(() ->
            new JdbcDatabaseClient(dataSource.get())); // ether-jdbc

    public DatabaseClient db() { return db.get(); }
}
```

---

## Más información

- [Guía ether-jdbc](ether-jdbc.md) — implementación JDBC
- [Guía ether-database-postgres](ether-database-postgres.md) — clasificación de errores Postgres
- [Javadoc API](../api/doxygen/html/index.html)
