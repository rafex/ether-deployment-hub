# Guía práctica: ether-config

**ether-config** provee carga y binding de configuración desde múltiples fuentes
(variables de entorno, propiedades del sistema, archivos JSON/TOML) sin frameworks externos.

## Instalación

```xml
<dependency>
    <groupId>dev.rafex.ether.config</groupId>
    <artifactId>ether-config</artifactId>
    <version>8.0.0-SNAPSHOT</version>
</dependency>
```

---

## Cargar configuración

```java
EtherConfig config = EtherConfig.of(
    new EnvironmentConfigSource(),          // variables de entorno
    new SystemPropertyConfigSource(),       // -D propiedades JVM
    new JsonFileConfigSource("config.json") // archivo JSON
);

// Obtener valor opcional
Optional<String> port = config.get("server.port");

// Obtener valor requerido (lanza excepción si falta)
String dbUrl = config.require("database.url");

// Snapshot completo
Map<String, String> all = config.snapshot();
```

---

## Binding a Records

Define tu configuración como Java Records:

```java
@ConfigPrefix("server")
record ServerConfig(int port, String host) {}

@ConfigPrefix("database")
record DatabaseConfig(String url, String user, String password, int poolSize) {}
```

Binds automáticamente variables de entorno `SERVER_PORT`, `SERVER_HOST`, etc.:

```java
ServerConfig   server = config.bind(ServerConfig.class);
DatabaseConfig db     = config.bind(DatabaseConfig.class);

System.out.println(server.port());  // 8080
System.out.println(db.url());       // jdbc:postgresql://...
```

### Binding con validación

```java
ServerConfig server = config.bindValidated(ServerConfig.class);
// Lanza excepción si algún campo requerido falta
```

### Alias de campos

```java
record AppConfig(
    @ConfigAlias("APP_SECRET_KEY") String secretKey,
    @ConfigAlias("APP_ENV") String environment
) {}
```

---

## Fuentes de configuración disponibles

| Clase | Descripción |
|---|---|
| `EnvironmentConfigSource` | Variables de entorno del sistema |
| `SystemPropertyConfigSource` | Propiedades `-Dclave=valor` de la JVM |
| `JsonFileConfigSource` | Archivo `.json` local |
| `TomlFileConfigSource` | Archivo `.toml` local |
| `SecretConfigSource` | Secretos externos (vault, etc.) |

Las fuentes se consultan en orden: la primera que tenga el valor gana.

---

## Tipos soportados en binding

| Java type | Ejemplo de valor |
|---|---|
| `String` | `"texto"` |
| `int` / `Integer` | `"8080"` |
| `long` / `Long` | `"1000000"` |
| `boolean` / `Boolean` | `"true"` |
| `double` / `Double` | `"3.14"` |
| `Duration` | `"PT30S"`, `"30s"` |
| `URI` | `"https://api.example.com"` |
| Enum | Nombre del valor |

---

## Patrón 12-factor completo

```java
// AppContainer.java
public class AppContainer {

    private final Lazy<EtherConfig>    config = new Lazy<>(() ->
            EtherConfig.of(new EnvironmentConfigSource()));
    private final Lazy<ServerConfig>   server = new Lazy<>(() ->
            config.get().bind(ServerConfig.class));
    private final Lazy<DatabaseConfig> db     = new Lazy<>(() ->
            config.get().bind(DatabaseConfig.class));

    public ServerConfig   serverConfig()   { return server.get(); }
    public DatabaseConfig databaseConfig() { return db.get(); }
}
```

```bash
# En producción
SERVER_PORT=8080 SERVER_HOST=0.0.0.0 DATABASE_URL=jdbc:postgresql://... java -jar app.jar
```

---

## Más información

- [Javadoc API](../api/doxygen/html/index.html)
- [Código fuente](https://github.com/rafex/ether-config)
