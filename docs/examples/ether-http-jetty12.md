# Guía práctica: ether-http-jetty12

**ether-http-jetty12** es el servidor HTTP de producción del ecosistema Ether, basado en Jetty 12.
Integra el sistema de rutas, middlewares, autenticación JWT, health checks y WebSockets.

## Instalación

```xml
<dependency>
    <groupId>dev.rafex.ether.http.jetty12</groupId>
    <artifactId>ether-http-jetty12</artifactId>
    <version>8.0.0-SNAPSHOT</version>
</dependency>
```

---

## Arrancar el servidor

```java
JettyServerConfig config = JettyServerConfig.builder()
    .port(8080)
    .host("0.0.0.0")
    .build();

JettyServerRunner runner = JettyServerFactory.create(config)
    .module(new AppModule(container))
    .build();

runner.start();
runner.await(); // bloquea hasta shutdown
```

---

## `JettyModule` — registro de rutas y middlewares

```java
public class AppModule implements JettyModule {

    private final AppContainer container;

    public AppModule(AppContainer container) {
        this.container = container;
    }

    @Override
    public void configure(JettyModuleContext ctx) {
        // Middlewares (en orden de ejecución)
        ctx.middleware(new RequestIdMiddleware());
        ctx.middleware(new LoggingMiddleware());
        ctx.middleware(new SecurityHeadersMiddleware(container.securityProfile()));
        ctx.middleware(new CorsMiddleware(container.securityProfile().cors()));

        // Rutas públicas
        ctx.route(Route.of("/health",    Set.of("GET")),    new HealthResource(container));
        ctx.route(Route.of("/openapi",   Set.of("GET")),    new OpenApiResource(container));

        // Rutas protegidas — requieren JWT
        ctx.authPolicy(new JwtAuthPolicy(container.tokenVerifier()));
        ctx.route(Route.of("/users",     Set.of("GET","POST")),       new UsersResource(container));
        ctx.route(Route.of("/users/{id}",Set.of("GET","PUT","DELETE")),new UserResource(container));
    }
}
```

---

## `JettyApiResponses` — respuestas estándar

```java
// En un HttpResource
@Override
public boolean get(HttpExchange x) {
    Optional<User> user = userService.findById(Long.parseLong(x.pathParam("id")));
    if (user.isEmpty()) {
        JettyApiErrorResponses.notFound(x, "Usuario no encontrado");
        return true;
    }
    JettyApiResponses.ok(x, user.get());
    return true;
}
```

---

## `JettyAuthHandler` — autenticación Bearer JWT

```java
// Se configura a través de JettyAuthPolicyRegistry
ctx.authPolicy(new JwtAuthPolicy(container.tokenVerifier()));

// Rutas sin auth — excluir explícitamente
ctx.publicRoute(Route.of("/health", Set.of("GET")));
ctx.publicRoute(Route.of("/openapi", Set.of("GET")));
```

---

## `EnhancedHealthHandler` — health checks integrados

```java
// Registra tus ProbeCheck en el módulo
ctx.livenessCheck(() -> ProbeResult.up("app"));
ctx.readinessCheck(() -> {
    try {
        container.db().query(SqlQuery.of("SELECT 1"), rs -> null);
        return ProbeResult.up("database");
    } catch (Exception e) {
        return ProbeResult.down("database", e.getMessage());
    }
});

// Endpoints disponibles automáticamente:
// GET /health/live  → {"status":"UP","kind":"LIVENESS",...}
// GET /health/ready → {"status":"UP","kind":"READINESS",...}
```

---

## `JettyResponseUtil` — respuestas de bajo nivel

```java
JettyResponseUtil.json(response, 200, body, codec);
JettyResponseUtil.text(response, 200, "texto");
JettyResponseUtil.noContent(response, 204);
JettyResponseUtil.problemJson(response, 404, problemDetails, codec);
```

---

## Patrón completo con ether-di

```java
public class Main {
    public static void main(String[] args) {
        var runtime = Bootstrap.start(AppContainer::new, AppContainer::warmup);
        AppContainer container = runtime.container();

        JettyServerRunner server = JettyServerFactory
            .create(container.serverConfig())
            .module(new AppModule(container))
            .build();

        server.start();
        server.await();
    }
}
```

---

## Más información

- [Guía ether-http-core](ether-http-core.md) — contratos `HttpResource`, `Route`, `Middleware`
- [Guía ether-http-security](ether-http-security.md) — CORS, headers, rate limiting
- [Guía ether-websocket-jetty12](ether-websocket-jetty12.md) — WebSockets en Jetty
- [Guía ether-glowroot-jetty12](ether-glowroot-jetty12.md) — APM con Glowroot
- [Ejemplo real: Kiwi](kiwi.md) — aplicación de producción completa
- [Javadoc API](../api/doxygen/html/index.html)
