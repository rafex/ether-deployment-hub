# Cómo extender el proyecto generado

Esta guía explica los patrones que debes seguir para agregar nuevas funcionalidades respetando la arquitectura hexagonal.

---

## Agregar una nueva entidad de dominio

### 1. Definir la entidad en `ports`

```java
// {app}-ports/src/main/java/{package}/domain/Product.java
package com.example.myapp.domain;

public record Product(
    Long id,
    String name,
    String sku,
    java.math.BigDecimal price
) {}
```

> Los records de Java son ideales para entidades de dominio: inmutables y con `equals`/`hashCode` automáticos.

### 2. Definir el puerto de salida en `ports`

```java
// {app}-ports/src/main/java/{package}/repository/ProductRepository.java
package com.example.myapp.repository;

import com.example.myapp.domain.Product;
import com.example.myapp.errors.AppError;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Optional<Product> findById(long id) throws AppError;
    List<Product> findAll() throws AppError;
    Product save(Product product) throws AppError;
    void deleteById(long id) throws AppError;
}
```

### 3. Implementar el repositorio en `infra-postgres`

```java
// {app}-infra-postgres/src/main/java/{package}/repository/impl/ProductRepositoryImpl.java
package com.example.myapp.repository.impl;

import com.example.myapp.domain.Product;
import com.example.myapp.errors.AppError;
import com.example.myapp.repository.ProductRepository;

import dev.rafex.ether.database.DatabaseClient;

import java.util.List;
import java.util.Optional;

public class ProductRepositoryImpl implements ProductRepository {

    private final DatabaseClient db;

    public ProductRepositoryImpl(DatabaseClient db) {
        this.db = db;
    }

    @Override
    public Optional<Product> findById(long id) throws AppError {
        try {
            return db.queryOne(
                "SELECT id, name, sku, price FROM products WHERE id = ?",
                rs -> new Product(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("sku"),
                    rs.getBigDecimal("price")
                ),
                id
            );
        } catch (Exception e) {
            throw new AppError("Error al buscar producto con id=" + id, e);
        }
    }

    // ... findAll, save, deleteById
}
```

---

## Agregar un nuevo caso de uso (servicio)

### 1. Definir la interfaz en `core`

```java
// {app}-core/src/main/java/{package}/services/ProductService.java
package com.example.myapp.services;

import com.example.myapp.domain.Product;
import com.example.myapp.errors.AppError;

import java.util.List;
import java.util.Optional;

public interface ProductService {
    Optional<Product> findById(long id) throws AppError;
    List<Product> findAll() throws AppError;
    Product create(Product product) throws AppError;
    void delete(long id) throws AppError;
}
```

### 2. Implementar el servicio en `core`

```java
// {app}-core/src/main/java/{package}/services/impl/ProductServiceImpl.java
package com.example.myapp.services.impl;

import com.example.myapp.domain.Product;
import com.example.myapp.errors.AppError;
import com.example.myapp.repository.ProductRepository;
import com.example.myapp.services.ProductService;

import java.util.List;
import java.util.Optional;

public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Optional<Product> findById(long id) throws AppError {
        return productRepository.findById(id);
    }

    @Override
    public List<Product> findAll() throws AppError {
        return productRepository.findAll();
    }

    @Override
    public Product create(Product product) throws AppError {
        // validaciones de negocio aquí
        if (product.name() == null || product.name().isBlank()) {
            throw new AppError("El nombre del producto no puede estar vacío");
        }
        return productRepository.save(product);
    }

    @Override
    public void delete(long id) throws AppError {
        productRepository.deleteById(id);
    }
}
```

---

## Registrar el nuevo componente en `bootstrap`

El contenedor de DI (`AppContainer`) necesita conocer las nuevas clases:

```java
// en AppContainer.java — agregar campos
private final Lazy<ProductRepository> productRepository;
private final Lazy<ProductService> productService;

// en el constructor
public AppContainer(AppConfig config, Overrides overrides) {
    // ... existentes ...

    this.productRepository = new Lazy<>(() ->
        overrides.productRepository() != null
            ? overrides.productRepository()
            : new ProductRepositoryImpl(db.get().client())
    );

    this.productService = new Lazy<>(() ->
        new ProductServiceImpl(productRepository.get())
    );
}

// getters
public ProductRepository productRepository() { return productRepository.get(); }
public ProductService productService() { return productService.get(); }
```

Agregar al record `Overrides`:

```java
public record Overrides(
    ExampleRepository exampleRepository,
    ProductRepository productRepository   // nuevo
) {
    public static Overrides none() {
        return new Overrides(null, null);
    }
}
```

---

## Agregar un nuevo handler HTTP en `transport-jetty`

### 1. Crear el handler

```java
// {app}-transport-jetty/src/main/java/{package}/handlers/ProductHandler.java
package com.example.myapp.handlers;

import com.example.myapp.errors.AppError;
import com.example.myapp.services.ProductService;

import dev.rafex.ether.http.jetty12.AbstractHandler;
import dev.rafex.ether.json.JsonMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;

import java.io.IOException;

public class ProductHandler extends AbstractHandler {

    private final ProductService productService;

    public ProductHandler(ProductService productService) {
        this.productService = productService;
    }

    @Override
    public void handle(String target, Request baseRequest,
                       HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        if (!target.startsWith("/products")) return;

        try {
            if ("GET".equals(request.getMethod()) && "/products".equals(target)) {
                var products = productService.findAll();
                response.setStatus(200);
                response.setContentType("application/json");
                response.getWriter().print(JsonMapper.toJson(products));
                baseRequest.setHandled(true);
            }
            // agregar más rutas...
        } catch (AppError e) {
            response.setStatus(500);
            response.getWriter().print("{\"error\":\"" + e.getMessage() + "\"}");
            baseRequest.setHandled(true);
        }
    }
}
```

### 2. Registrar el handler en `AppServer`

```java
// en AppServer.java
public AppServer(ServerConfig config, AppContainer container) {
    server = new Server(config.port());

    HandlerList handlers = new HandlerList(
        new HealthHandler(),
        new ProductHandler(container.productService()),  // nuevo
        new ExampleHandler(container.exampleService())
    );
    server.setHandler(handlers);
}
```

---

## Agregar un adaptador de transporte nuevo (ej. gRPC)

El módulo `{app}-transport-grpc` ya existe como placeholder. Para activarlo:

### 1. Agregar dependencias en su `pom.xml`

```xml
<dependencies>
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-netty-shaded</artifactId>
        <version>${grpc.version}</version>
    </dependency>
    <!-- ... -->
    <dependency>
        <groupId>${project.groupId}</groupId>
        <artifactId>${rootArtifactId}-core</artifactId>
        <version>${project.version}</version>
    </dependency>
</dependencies>
```

### 2. Implementar el servicio gRPC

```java
// {app}-transport-grpc/src/main/java/{package}/transport/grpc/ProductGrpcService.java
package com.example.myapp.transport.grpc;

import com.example.myapp.services.ProductService;

// implementa el stub gRPC generado
public class ProductGrpcService extends ProductServiceGrpc.ProductServiceImplBase {
    private final ProductService productService;

    public ProductGrpcService(ProductService productService) {
        this.productService = productService;
    }
    // ...
}
```

### 3. Actualizar el `archetype-metadata.xml` si es necesario

Si agregas recursos adicionales (`.proto`, configuración), actualiza el `fileSet` correspondiente en el arquetipo.

---

## Agregar configuración nueva

Si necesitas una nueva sección de configuración (ej. `RedisConfig`):

### 1. Crear el record en `common`

```java
// {app}-common/src/main/java/{package}/config/RedisConfig.java
package com.example.myapp.config;

public record RedisConfig(String host, int port, int database) {

    public static RedisConfig fromEnv() {
        return new RedisConfig(
            System.getenv().getOrDefault("REDIS_HOST", "localhost"),
            Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379")),
            Integer.parseInt(System.getenv().getOrDefault("REDIS_DB", "0"))
        );
    }
}
```

### 2. Agregar al `AppConfig`

```java
public record AppConfig(
    DatabaseConfig database,
    ServerConfig server,
    RedisConfig redis          // nuevo
) {
    public static AppConfig fromEnv() {
        return new AppConfig(
            DatabaseConfig.fromEnv(),
            ServerConfig.fromEnv(),
            RedisConfig.fromEnv()
        );
    }
}
```

---

## Verificar que no se rompen las reglas

Después de cada cambio, ejecuta los tests de arquitectura:

```bash
make test
```

Si una clase en `core` accidentalmente importa algo de `infra`, verás un fallo como:

```
Architecture Violation [Priority: MEDIUM] - Rule 'no classes that reside in a package
'com.example.myapp.services..' should depend on classes that reside in any package
['com.example.myapp.repository.impl..', ...]' was violated
```

Esto te indica exactamente qué dependencia viola la arquitectura.
