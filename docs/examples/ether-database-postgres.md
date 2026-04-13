# Guía práctica: ether-database-postgres

**ether-database-postgres** añade clasificación semántica de errores PostgreSQL sobre
`DatabaseAccessException`, permitiendo manejar violaciones de unicidad, claves foráneas,
restricciones check y conflictos de concurrencia sin parsear códigos SQL a mano.

## Instalación

```xml
<dependency>
    <groupId>dev.rafex.ether.database.postgres</groupId>
    <artifactId>ether-database-postgres</artifactId>
    <version>8.0.0-SNAPSHOT</version>
</dependency>
```

---

## `PostgresErrorClassifier`

```java
try {
    db.execute(insertQuery);
} catch (DatabaseAccessException e) {
    PostgresErrorClassifier.Category cat = PostgresErrorClassifier.classify(
        (SQLException) e.getCause()
    );
    switch (cat) {
        case UNIQUE_VIOLATION      -> throw new ConflictException("El recurso ya existe");
        case FOREIGN_KEY_VIOLATION -> throw new BadRequestException("Referencia inválida");
        case NOT_NULL_VIOLATION    -> throw new BadRequestException("Campo requerido ausente");
        case CHECK_VIOLATION       -> throw new BadRequestException("Valor fuera de rango");
        case CONCURRENCY_CONFLICT  -> throw new RetryableException("Conflicto de concurrencia");
        case OTHER                 -> throw e; // relanzar si no es clasificable
    }
}
```

---

## Categorías disponibles

| Categoría | Código Postgres | Cuándo ocurre |
|---|---|---|
| `UNIQUE_VIOLATION` | `23505` | INSERT/UPDATE viola restricción UNIQUE o PK |
| `FOREIGN_KEY_VIOLATION` | `23503` | FK apunta a un registro que no existe |
| `NOT_NULL_VIOLATION` | `23502` | Campo NOT NULL recibe NULL |
| `CHECK_VIOLATION` | `23514` | Valor viola restricción CHECK |
| `CONCURRENCY_CONFLICT` | `40001` / `40P01` | Deadlock o serialization failure |
| `OTHER` | resto | Cualquier otro error SQL |

---

## `PostgresSqlStates` — constantes de códigos

```java
// Si necesitas comparar manualmente
String state = ((SQLException) e.getCause()).getSQLState();
if (PostgresSqlStates.UNIQUE_VIOLATION.equals(state)) { ... }
```

---

## Patrón de repositorio

```java
public class JdbcUserRepository implements UserRepository {

    private final DatabaseClient db;

    public JdbcUserRepository(DatabaseClient db) { this.db = db; }

    @Override
    public User create(String name, String email) {
        try {
            db.execute(SqlQuery.of(
                "INSERT INTO users (name, email) VALUES (?, ?)",
                List.of(SqlParameter.of(name), SqlParameter.of(email))
            ));
            return db.queryOne(
                SqlQuery.of("SELECT * FROM users WHERE email = ?",
                    List.of(SqlParameter.of(email))),
                this::mapRow
            ).orElseThrow();
        } catch (DatabaseAccessException e) {
            var cat = PostgresErrorClassifier.classify((SQLException) e.getCause());
            if (cat == UNIQUE_VIOLATION) throw new EmailAlreadyExistsException(email);
            throw e;
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(rs.getLong("id"), rs.getString("name"), rs.getString("email"));
    }
}
```

---

## Más información

- [Guía ether-database-core](ether-database-core.md) — contratos base
- [Guía ether-jdbc](ether-jdbc.md) — implementación JDBC
- [Javadoc API](../api/doxygen/html/index.html)
