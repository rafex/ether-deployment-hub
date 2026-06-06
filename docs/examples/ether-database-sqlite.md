# Guía práctica: ether-database-sqlite

**ether-database-sqlite** añade utilidades específicas de SQLite sobre la pila
`ether-database-core` + `ether-jdbc`: cliente JDBC especializado, parámetros
SQLite, clasificación de errores y configuración de PRAGMAs como WAL, foreign
keys y busy timeout.

## Instalación

```xml
<dependency>
    <groupId>dev.rafex.ether.database</groupId>
    <artifactId>ether-database-sqlite</artifactId>
    <version>9.5.5</version>
</dependency>
```

Para ejecutar contra SQLite también necesitas el driver JDBC en runtime:

```xml
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.45.3.0</version>
    <scope>runtime</scope>
</dependency>
```

---

## Cliente SQLite

```java
SQLiteConfig config = SQLiteConfig.builder()
    .journalMode(JournalMode.WAL)
    .synchronousMode(SynchronousMode.NORMAL)
    .foreignKeys(true)
    .busyTimeout(5000)
    .build();

SQLiteDatabaseClient db = SQLiteDatabaseClient.builder(dataSource)
    .withConfig(config)
    .build();
```

`SQLiteDatabaseClient` delega las operaciones base a `JdbcDatabaseClient`, pero
permite aplicar configuración SQLite al inicializar conexiones.

---

## PRAGMAs recomendados

```java
try (Connection connection = dataSource.getConnection()) {
    SQLitePragmas.apply(connection, SQLiteConfig.defaults());
}
```

Los modos principales son:

| Configuración | Uso |
|---|---|
| `JournalMode.WAL` | Mejora concurrencia de lecturas/escrituras |
| `SynchronousMode.NORMAL` | Balance entre rendimiento y durabilidad |
| `foreignKeys(true)` | Activa validación de claves foráneas |
| `busyTimeout(5000)` | Espera antes de fallar por base bloqueada |

---

## Parámetros SQLite

```java
SqlQuery query = SqlQuery.of(
    "INSERT INTO audit_log (payload, created_at) VALUES (?, ?)",
    List.of(
        SQLiteParameters.json("{\"event\":\"login\"}"),
        SQLiteParameters.integer(System.currentTimeMillis())
    )
);

db.execute(query);
```

`SQLiteParameters` cubre las clases de almacenamiento habituales: `BLOB`,
`INTEGER`, `REAL`, `TEXT` y JSON vía la extensión JSON1 cuando está disponible.

---

## Errores SQLite

```java
try {
    db.execute(insertQuery);
} catch (SQLException e) {
    throw SQLiteErrorClassifier.classify(e);
}
```

`SQLiteErrorCodes` expone constantes para códigos frecuentes como unique,
foreign key, not null, check, busy y locked.

---

## Más información

- [Guía ether-database-core](ether-database-core.md) — contratos base
- [Guía ether-jdbc](ether-jdbc.md) — implementación JDBC
- [Javadoc API](../api/doxygen/html/index.html)
