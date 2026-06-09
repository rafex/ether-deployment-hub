# Módulos del proyecto generado

Cada módulo tiene una responsabilidad única y estricta. A continuación se describe su contenido, propósito y las clases que genera el arquetipo como punto de partida.

---

## `{app}-ports`

**Rol:** Definir los contratos del dominio hacia el exterior (puertos de salida).

**Regla:** No puede depender de ningún otro módulo del proyecto.

### Clases generadas

#### `ExampleRepository` (interface)

```java
public interface ExampleRepository {
    Optional<ExampleEntity> findById(long id);
    List<ExampleEntity> findAll();
    ExampleEntity save(ExampleEntity entity);
    void deleteById(long id);
}
```

Con `ExampleEntity`:

```java
public record ExampleEntity(Long id, String name, String description) {}
```

### Cuándo agregar algo aquí

- Cuando defines un nuevo puerto de salida (repositorio, cliente de servicio externo, proveedor de eventos).
- Los puertos de entrada (casos de uso) también pueden vivir aquí si deseas exponerlos como interfaces.

---

## `{app}-common`

**Rol:** Configuración transversal, errores del dominio y utilidades compartidas por todas las capas.

**Regla:** No puede depender de `core`, `infra`, `transport` ni `bootstrap`.

### Clases generadas

#### `AppConfig` (record, singleton)

Carga la configuración desde variables de entorno y propiedades del sistema. Expone `DatabaseConfig` y `ServerConfig`.

```java
public record AppConfig(DatabaseConfig database, ServerConfig server) {
    private static volatile AppConfig INSTANCE;
    public static AppConfig getInstance() { ... }  // double-checked locking
}
```

#### `DatabaseConfig` (record)

```java
public record DatabaseConfig(
    String host, int port, String name,
    String user, String password,
    int poolSize, long connectionTimeout
) {}
```

Variables de entorno leídas: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_POOL_SIZE`, `DB_CONNECTION_TIMEOUT`.

#### `ServerConfig` (record)

```java
public record ServerConfig(String host, int port) {}
```

Variables: `SERVER_HOST`, `SERVER_PORT`.

#### `AppError` (checked exception)

Excepción comprobada de dominio. Úsala para errores esperados que el llamador debe manejar.

```java
public class AppError extends Exception {
    public AppError(String message) { ... }
    public AppError(String message, Throwable cause) { ... }
}
```

#### `AppRuntimeError` (unchecked exception)

Para errores de programación o condiciones irrecuperables.

```java
public class AppRuntimeError extends RuntimeException {
    public AppRuntimeError(String message) { ... }
    public AppRuntimeError(String message, Throwable cause) { ... }
}
```

---

## `{app}-core`

**Rol:** Implementar la lógica de negocio. Sólo puede depender de `ports` y `common`.

**Regla:** No puede importar clases de `infra`, `bootstrap`, `transport` ni ningún adaptador concreto.

### Clases generadas

#### `ExampleService` (interface)

Define los casos de uso disponibles:

```java
public interface ExampleService {
    Optional<ExampleEntity> findById(long id) throws AppError;
    List<ExampleEntity> findAll() throws AppError;
    ExampleEntity create(ExampleEntity entity) throws AppError;
    void delete(long id) throws AppError;
}
```

#### `ExampleServiceImpl`

Implementación que delega en `ExampleRepository` (el puerto):

```java
public class ExampleServiceImpl implements ExampleService {
    private final ExampleRepository repository;

    public ExampleServiceImpl(ExampleRepository repository) {
        this.repository = repository;
    }
    // ...
}
```

El repositorio se inyecta en el constructor → no hay acoplamiento a ninguna implementación concreta.

---

## `{app}-infra-postgres`

**Rol:** Adaptador de salida. Implementa los puertos usando PostgreSQL + HikariCP.

**Regla:** No puede importar clases de `transport`, `bootstrap` ni servicios del `core`.

### Clases generadas

#### `Db` (singleton de infraestructura)

Gestiona el pool de conexiones HikariCP y expone el `DatabaseClient` de Ether:

```java
public final class Db {
    private static volatile Db INSTANCE;

    public static Db getInstance(DatabaseConfig config) { ... }

    public DataSource dataSource() { ... }
    public DatabaseClient client() { ... }
    public void close() { ... }
}
```

#### `ExampleRepositoryImpl`

Implementa `ExampleRepository` usando `DatabaseClient`:

```java
public class ExampleRepositoryImpl implements ExampleRepository {
    private final DatabaseClient db;

    public ExampleRepositoryImpl(DatabaseClient db) {
        this.db = db;
    }
    // findById, findAll, save, deleteById...
}
```

---

## `{app}-bootstrap`

**Rol:** Contenedor de inyección de dependencias manual (sin frameworks de DI como Spring).

**Regla:** Puede depender de todos los módulos excepto `transport`.

### Clases generadas

#### `Lazy<T>`

Inicialización diferida thread-safe con doble verificación de bloqueo:

```java
public final class Lazy<T> {
    private final Supplier<T> supplier;
    private volatile T value;

    public T get() {
        if (value == null) {
            synchronized (this) {
                if (value == null) value = supplier.get();
            }
        }
        return value;
    }
}
```

#### `Closer`

Registra recursos `AutoCloseable` y los cierra en orden LIFO (último en registrar, primero en cerrar):

```java
public final class Closer implements AutoCloseable {
    private final Deque<AutoCloseable> resources = new ArrayDeque<>();

    public <T extends AutoCloseable> T register(T resource) {
        resources.push(resource);
        return resource;
    }

    @Override
    public void close() {
        // cierra en orden inverso al registro
    }
}
```

#### `AppContainer`

Ensambla todos los objetos de la aplicación. Usa `Lazy<T>` para inicialización diferida:

```java
public class AppContainer {
    private final Lazy<Db> db;
    private final Lazy<ExampleRepository> exampleRepository;
    private final Lazy<ExampleService> exampleService;

    public AppContainer(AppConfig config, Overrides overrides) { ... }

    // getters que devuelven las instancias lazy
}
```

El record `Overrides` permite reemplazar implementaciones en tests:

```java
public record Overrides(
    ExampleRepository exampleRepository
) {
    public static Overrides none() { return new Overrides(null); }
}
```

#### `AppBootstrap`

Punto de arranque de la aplicación: crea el contenedor, registra el shutdown hook y devuelve un `AppRuntime`:

```java
public final class AppBootstrap {
    public static AppRuntime start(AppConfig config, Overrides overrides) {
        Closer closer = new Closer();
        AppContainer container = new AppContainer(config, overrides);
        // registra recursos en closer
        Runtime.getRuntime().addShutdownHook(new Thread(closer::close));
        return new AppRuntime(container, closer);
    }
}

public record AppRuntime(AppContainer container, Closer closer) {}
```

---

## `{app}-transport-jetty`

**Rol:** Adaptador de entrada HTTP usando Jetty 12. Escucha peticiones y delega en Core.

**Regla:** No puede importar clases de `infra` directamente (usa Core a través de Ports).

### Clases generadas

#### `App` (main class)

```java
public class App {
    private static final Logger LOG = Logger.getLogger(App.class.getName());

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.getInstance();
        AppRuntime runtime = AppBootstrap.start(config, Overrides.none());
        AppServer server = new AppServer(config.server(), runtime.container());
        server.start();
        server.join();
    }
}
```

#### `AppServer`

Configura y arranca el servidor Jetty con los handlers registrados:

```java
public class AppServer {
    private final Server server;

    public AppServer(ServerConfig config, AppContainer container) {
        server = new Server(config.port());
        // registra HealthHandler y demás handlers
    }

    public void start() throws Exception { server.start(); }
    public void join() throws InterruptedException { server.join(); }
    public void stop() throws Exception { server.stop(); }
}
```

#### `HealthHandler`

Handler de ejemplo con el endpoint `GET /health`:

```java
public class HealthHandler extends AbstractHandler {
    @Override
    public void handle(String target, Request baseRequest,
                       HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if ("/health".equals(target) && "GET".equals(request.getMethod())) {
            response.setStatus(200);
            response.setContentType("application/json");
            response.getWriter().print("{\"status\":\"UP\"}");
            baseRequest.setHandled(true);
        }
    }
}
```

### Recursos generados

- `src/main/resources/application.properties` — propiedades de la aplicación

---

## `{app}-transport-grpc`

**Rol:** Placeholder para un adaptador de entrada gRPC.

Contiene únicamente `package-info.java`. Añade aquí tus servicios gRPC cuando los necesites.

---

## `{app}-transport-rabbitmq`

**Rol:** Placeholder para un adaptador de entrada/salida RabbitMQ (mensajería).

Contiene únicamente `package-info.java`. Implementa consumidores y publicadores aquí.

---

## `{app}-tools`

**Rol:** Scripts auxiliares, seeds de base de datos, herramientas de desarrollo.

Contiene únicamente `package-info.java`. Úsalo para:
- Programas `main()` de seed/migración
- Clientes de prueba (smoke tests manuales)
- Herramientas de mantenimiento

---

## `{app}-architecture-tests`

**Rol:** Verificar en tiempo de compilación que las reglas de la arquitectura hexagonal se respetan.

### Dependencias

```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <scope>test</scope>
</dependency>
```

Depende de **todos** los demás módulos para poder analizar las clases de cada capa.

### Cómo funciona ArchUnit

ArchUnit carga el bytecode de todos los módulos y ejecuta reglas sobre el grafo de dependencias. Si una clase en `core` importa una clase de `infra`, el test falla en `mvn test`.

```java
@AnalyzeClasses(packages = "com.example.myapp")
public class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule core_must_not_depend_on_adapters = noClasses()
        .that().resideInAPackage("com.example.myapp.services..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "com.example.myapp.repository.impl..",
            "com.example.myapp.db..",
            "com.example.myapp.bootstrap..",
            "com.example.myapp.handlers..",
            ...
        );
}
```

### Reglas implementadas (6)

Ver [architecture.md](architecture.md#reglas-archunit-implementadas) para la descripción completa.
