# ether-logging-core

Utilidades ligeras de logging sobre `java.util.logging` (JUL) para el ecosistema Ether.
Sin dependencias externas. Proporciona una API de conveniencia, configuración programática
y un formateador con salida ISO-8601 en UTC.

## Maven Dependency

```xml
<dependency>
    <groupId>dev.rafex.ether.logging</groupId>
    <artifactId>ether-logging-core</artifactId>
    <version>8.0.0-SNAPSHOT</version>
</dependency>
```

---

## `EtherLog` — API de logging estática

`EtherLog` instancia loggers de JUL por clase usando `ClassValue` para evitar la búsqueda
repetida en el mapa. La interpolación usa `{}` como marcador de posición.

```java
public class UserService {

    private static final EtherLog LOG = EtherLog.getLogger(UserService.class);

    public User findById(long id) {
        LOG.debug("Buscando usuario con id={}", id);
        try {
            User user = repository.findById(id);
            LOG.info("Usuario encontrado: {}", user.email());
            return user;
        } catch (Exception e) {
            LOG.error("Error al buscar usuario {}: {}", id, e.getMessage());
            throw e;
        }
    }
}
```

### Niveles disponibles

```java
LOG.trace("Detalle de traza: {}", value);   // FINER en JUL
LOG.debug("Diagnóstico: {}", value);        // FINE en JUL
LOG.info("Evento normal: {}", value);       // INFO en JUL
LOG.warn("Advertencia: {}", value);         // WARNING en JUL
LOG.error("Error: {}", value);             // SEVERE en JUL
```

---

## `LoggingConfigurator` — configuración programática

Configura el root logger de JUL en el arranque de la aplicación:

```java
// Nivel INFO por defecto, formateador de Ether
LoggingConfigurator.configureRootLogger();

// Nivel explícito
LoggingConfigurator.configureRootLogger(Level.FINE);
```

Llamar a `configureRootLogger()` antes de cualquier otro código garantiza que
todos los loggers hereden el formateador y el nivel correcto.

### Integración en el arranque con ether-di

```java
public class AppContainer {

    public AppContainer() {
        // Primera línea del constructor — antes de cualquier LOG
        LoggingConfigurator.configureRootLogger();
    }
}
```

---

## `LogLevels` — parseo de nivel desde variables de entorno

`LogLevels.parse()` convierte cadenas de texto (incluidos nombres populares de otros
frameworks) a niveles JUL:

| Entrada (case-insensitive) | Nivel JUL |
|---|---|
| `"TRACE"` | `FINER` |
| `"DEBUG"` | `FINE` |
| `"INFO"` | `INFO` |
| `"WARN"`, `"WARNING"` | `WARNING` |
| `"ERROR"`, `"SEVERE"` | `SEVERE` |
| `"OFF"` | `OFF` |
| Valor desconocido | `INFO` (por defecto) |

```java
// Leer el nivel desde variable de entorno — patrón 12-factor
Level level = LogLevels.parse(System.getenv("LOG_LEVEL"));
LoggingConfigurator.configureRootLogger(level);
```

```bash
# En producción
LOG_LEVEL=INFO java -jar app.jar

# En desarrollo
LOG_LEVEL=DEBUG java -jar app.jar
```

---

## `EtherLogFormatter` — formato de salida

Cada entrada de log se emite en una sola línea con este formato:

```
2025-06-01T14:32:05.123Z INFO  dev.rafex.app.UserService - Usuario encontrado: alice@example.com
```

Campos:
- **Timestamp**: ISO-8601 en UTC (`yyyy-MM-dd'T'HH:mm:ss.SSS'Z'`)
- **Nivel**: alineado a 7 caracteres (`SEVERE `, `WARNING`, `INFO   `, `FINE   `, `FINER  `)
- **Logger**: nombre completo de la clase
- **Mensaje**: con marcadores `{}` ya interpolados

Si el log incluye un `Throwable`, el stack trace completo se añade en líneas siguientes.

---

## Ejemplo completo de arranque

```java
// Main.java
public class Main {

    public static void main(String[] args) {
        // 1. Configurar logging antes de todo
        Level level = LogLevels.parse(System.getenv("LOG_LEVEL"));
        LoggingConfigurator.configureRootLogger(level);

        // 2. Arrancar la aplicación
        var runtime = Bootstrap.start(AppContainer::new, AppContainer::warmup);
        JettyServer.start(runtime.container());
    }
}
```

---

## Compatibilidad

- Java 21+
- Sin dependencias externas (solo `java.util.logging`)
- Compatible con GraalVM native-image

---

## Comparación con alternativas

| | ether-logging-core | SLF4J + Logback | Log4j2 |
|---|---|---|---|
| Dependencias | 0 | ~800 KB | ~1 MB+ |
| Configuración en arranque | Programática | XML / Groovy | XML / JSON |
| GraalVM native | Sin config | Requiere hints | Requiere hints |
| API de conveniencia (`{}`) | Sí | Sí | Sí |
| Rotación de ficheros | No (usa JUL handlers) | Sí | Sí |

> Usa `ether-logging-core` en servicios simples y herramientas CLI. Para aplicaciones
> que requieren rotación de logs o configuración externa compleja, considera añadir
> un backend SLF4J sobre JUL.
