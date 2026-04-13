# Guía práctica: ether-jdbc

**ether-jdbc** provee la implementación JDBC de `DatabaseClient` de `ether-database-core`,
con soporte para pool de conexiones vía HikariCP.

## Instalación

```xml
<dependency>
    <groupId>dev.rafex.ether.jdbc</groupId>
    <artifactId>ether-jdbc</artifactId>
    <version>8.0.0-SNAPSHOT</version>
</dependency>
```

---

## `JdbcDatabaseClient`

```java
// Con cualquier DataSource (HikariCP, DBCP, simple, etc.)
DataSource    dataSource = buildDataSource();
DatabaseClient db        = new JdbcDatabaseClient(dataSource);

// Consulta
List<User> users = db.queryList(
    SqlQuery.of("SELECT id, name FROM users"),
    rs -> new User(rs.getLong("id"), rs.getString("name"))
);
```

---

## `SimpleDataSource` — sin pool (desarrollo / tests)

```java
DataSource ds = new SimpleDataSource(
    "jdbc:postgresql://localhost:5432/mydb",
    "user",
    "password"
);
DatabaseClient db = new JdbcDatabaseClient(ds);
```

---

## Con HikariCP (producción)

```java
HikariConfig hikari = new HikariConfig();
hikari.setJdbcUrl("jdbc:postgresql://localhost:5432/mydb");
hikari.setUsername("user");
hikari.setPassword("password");
hikari.setMaximumPoolSize(10);
hikari.setMinimumIdle(2);
hikari.setConnectionTimeout(30_000);

DataSource     ds = new HikariDataSource(hikari);
DatabaseClient db = new JdbcDatabaseClient(ds);
```

---

## Integración con ether-di y ether-config

```java
public class AppContainer implements AutoCloseable {

    private final Closer closer = new Closer();

    private final Lazy<DatabaseConfig> dbConfig = new Lazy<>(() ->
            config.get().bind(DatabaseConfig.class));

    private final Lazy<DataSource> dataSource = new Lazy<>(() -> {
        HikariConfig hk = new HikariConfig();
        hk.setJdbcUrl(dbConfig.get().url());
        hk.setUsername(dbConfig.get().user());
        hk.setPassword(dbConfig.get().password());
        hk.setMaximumPoolSize(dbConfig.get().poolSize());
        return closer.register(new HikariDataSource(hk));
    });

    private final Lazy<DatabaseClient> db = new Lazy<>(() ->
            new JdbcDatabaseClient(dataSource.get()));

    public DatabaseClient db() { return db.get(); }

    @Override public void close() { closer.close(); }
}
```

---

## Más información

- [Guía ether-database-core](ether-database-core.md) — contratos `DatabaseClient`, `SqlQuery`, `RowMapper`
- [Guía ether-database-postgres](ether-database-postgres.md) — clasificación de errores Postgres
- [Javadoc API](../api/doxygen/html/index.html)
