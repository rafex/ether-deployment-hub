# Guía práctica: ether-glowroot-jetty12

**ether-glowroot-jetty12** integra el agente APM [Glowroot](https://glowroot.org) con el servidor
Jetty 12 de Ether, añadiendo visibilidad de transacciones, tiempos de respuesta, contexto de usuario
y request IDs sin modificar el código de dominio.

## Instalación

```xml
<dependency>
    <groupId>dev.rafex.ether.glowroot.jetty12</groupId>
    <artifactId>ether-glowroot-jetty12</artifactId>
    <version>8.0.0-SNAPSHOT</version>
</dependency>
```

> Glowroot también requiere el agente Java: `-javaagent:glowroot/glowroot.jar`

---

## Middlewares disponibles

| Clase | Propósito |
|---|---|
| `GlowrootHttpMiddleware` | Middleware principal — marca transacciones HTTP |
| `GlowrootSlowThresholdMiddleware` | Detecta y etiqueta requests lentos |
| `GlowrootHealthExclusionMiddleware` | Excluye `/health/**` de las métricas |
| `GlowrootAuthUserMiddleware` | Propaga el usuario autenticado al contexto APM |
| `GlowrootRequestIdMiddleware` | Añade el request ID al span de Glowroot |
| `GlowrootStatusCapturingMiddleware` | Captura el código HTTP de respuesta |

---

## Registrar los middlewares en `JettyModule`

```java
public class AppModule implements JettyModule {

    @Override
    public void configure(JettyModuleContext ctx) {
        // APM — registrar primero para capturar todo el pipeline
        ctx.middleware(new GlowrootHttpMiddleware());
        ctx.middleware(new GlowrootHealthExclusionMiddleware());
        ctx.middleware(new GlowrootRequestIdMiddleware());
        ctx.middleware(new GlowrootAuthUserMiddleware());
        ctx.middleware(new GlowrootStatusCapturingMiddleware());
        ctx.middleware(new GlowrootSlowThresholdMiddleware(Duration.ofSeconds(2)));

        // Resto de middlewares de la aplicación
        ctx.middleware(new LoggingMiddleware());
        ctx.middleware(new CorsMiddleware(container.securityProfile().cors()));

        // Rutas
        ctx.route(Route.of("/users", Set.of("GET", "POST")), new UsersResource(container));
    }
}
```

---

## `GlowrootWebSocketEndpointWrapper` — APM para WebSockets

```java
// Envuelve el endpoint para instrumentar transacciones WS
ctx.websocket(WebSocketRoute.of("/chat/{room}",
    new GlowrootWebSocketEndpointWrapper(
        new ChatEndpoint(),
        "chat"  // nombre de la transacción en Glowroot
    )
));
```

---

## `GlowrootJettyHandler` — integración de bajo nivel

```java
// Usado internamente por JettyServerFactory cuando detecta Glowroot
// No necesitas configurarlo manualmente en la mayoría de casos
Handler gHandler = new GlowrootJettyHandler(appHandler);
```

---

## Arrancar con el agente Glowroot

```bash
java \
  -javaagent:glowroot/glowroot.jar \
  -Dglowroot.data.dir=/var/glowroot \
  -jar app.jar
```

Glowroot expone su interfaz en `http://localhost:4000` por defecto.

---

## Qué se ve en Glowroot

- **Transacciones HTTP** por ruta (`GET /users`, `POST /users/{id}`, etc.)
- **Tiempo de respuesta** con percentiles (P50, P95, P99)
- **Queries SQL** (si usas `ether-jdbc`) con tiempo y texto
- **Errores** clasificados por tipo y stack trace
- **Usuario autenticado** en cada transacción (via `GlowrootAuthUserMiddleware`)
- **Request ID** para correlación con logs
- **Requests lentos** destacados según el umbral configurado

---

## Más información

- [Javadoc API](../api/doxygen/html/index.html)
- [Glowroot documentación oficial](https://glowroot.org)
- [Guía ether-http-jetty12](ether-http-jetty12.md) — servidor HTTP
- [Guía ether-websocket-jetty12](ether-websocket-jetty12.md) — WebSockets
