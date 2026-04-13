# Guía práctica: ether-logging-core

**ether-logging-core** provee utilidades ligeras de logging sobre `java.util.logging` (JUL).
Sin dependencias externas, con configuración programática y formato ISO-8601 en UTC.

## Instalación

```xml
<dependency>
    <groupId>dev.rafex.ether.logging</groupId>
    <artifactId>ether-logging-core</artifactId>
    <version>8.0.0-SNAPSHOT</version>
</dependency>
```

## Las tres clases

| Clase | Propósito |
|---|---|
| `EtherLog` | API de logging estática con interpolación `{}` y caché por clase |
| `LoggingConfigurator` | Configura el root logger de JUL en el arranque |
| `LogLevels` | Convierte cadenas de texto (`"DEBUG"`, `"INFO"`, …) a niveles JUL |
| `EtherLogFormatter` | Formateador con salida ISO-8601 en UTC en una sola línea |

---

## EtherLog — API de logging

```java
public class UserService {

    private static final EtherLog LOG = EtherLog.getLogger(UserService.class);

    public User findById(long id) {
        LOG.debug("Buscando usuario id={}", id);
        User user = repository.findById(id);
        LOG.info("Usuario encontrado: {}", user.email());
        return user;
    }
}
```

### Niveles

```java
LOG.trace("detalle: {}", value);   // JUL FINER
LOG.debug("diagnóstico: {}", value); // JUL FINE
LOG.info("evento: {}", value);     // JUL INFO
LOG.warn("advertencia: {}", value);  // JUL WARNING
LOG.error("error: {}", value);    // JUL SEVERE
```

---

## LoggingConfigurator — configuración en el arranque

```java
// Nivel INFO por defecto con EtherLogFormatter
LoggingConfigurator.configureRootLogger();

// Nivel explícito
LoggingConfigurator.configureRootLogger(Level.FINE);
```

Llama a `configureRootLogger()` **antes** de cualquier logger para que todos hereden
el formateador correcto.

---

## LogLevels — nivel desde variable de entorno

```java
Level level = LogLevels.parse(System.getenv("LOG_LEVEL"));
LoggingConfigurator.configureRootLogger(level);
```

| Variable `LOG_LEVEL` | Nivel JUL resultante |
|---|---|
| `TRACE` | `FINER` |
| `DEBUG` | `FINE` |
| `INFO` | `INFO` |
| `WARN` / `WARNING` | `WARNING` |
| `ERROR` / `SEVERE` | `SEVERE` |
| desconocido / vacío | `INFO` |

```bash
LOG_LEVEL=DEBUG java -jar app.jar
```

---

## EtherLogFormatter — formato de salida

```
2025-06-01T14:32:05.123Z INFO    dev.rafex.app.UserService - Usuario encontrado: alice@example.com
```

- Timestamp ISO-8601 en UTC
- Nivel alineado a 7 caracteres
- Nombre completo del logger
- Mensaje con marcadores `{}` ya interpolados
- Stack trace en líneas adicionales si hay `Throwable`

---

## Patrón completo de arranque

```java
public class Main {

    public static void main(String[] args) {
        // 1. Logging primero
        Level level = LogLevels.parse(System.getenv("LOG_LEVEL"));
        LoggingConfigurator.configureRootLogger(level);

        // 2. Arrancar la aplicación
        var runtime = Bootstrap.start(AppContainer::new, AppContainer::warmup);
        JettyServer.start(runtime.container());
    }
}
```

---

## Más información

- [Javadoc API](../api/doxygen/html/index.html) — documentación completa de clases y métodos
- [Código fuente](https://github.com/rafex/ether-logging-core)
