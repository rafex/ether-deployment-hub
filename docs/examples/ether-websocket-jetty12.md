# Guía práctica: ether-websocket-jetty12

**ether-websocket-jetty12** integra los contratos de `ether-websocket-core` con Jetty 12,
permitiendo registrar `WebSocketEndpoint` en el mismo servidor que los endpoints HTTP.

## Instalación

```xml
<dependency>
    <groupId>dev.rafex.ether.websocket.jetty12</groupId>
    <artifactId>ether-websocket-jetty12</artifactId>
    <version>8.0.0-SNAPSHOT</version>
</dependency>
```

---

## Registrar WebSockets en un `JettyModule`

```java
public class AppModule implements JettyModule {

    private final AppContainer container;

    @Override
    public void configure(JettyModuleContext ctx) {
        // Rutas HTTP
        ctx.route(Route.of("/users", Set.of("GET", "POST")), new UsersResource(container));

        // Rutas WebSocket
        ctx.websocket(WebSocketRoute.of("/chat/{room}",   new ChatEndpoint()));
        ctx.websocket(WebSocketRoute.of("/notifications", new NotificationsEndpoint(container)));
        ctx.websocket(WebSocketRoute.of("/live/{stream}", new LiveDataEndpoint(container)));
    }
}
```

---

## Servidor con HTTP + WebSocket

```java
JettyServerRunner runner = JettyServerFactory.create(serverConfig)
    .module(new AppModule(container))
    .build();

runner.start();
// ws://host:8080/chat/general
// ws://host:8080/notifications
runner.await();
```

---

## Autenticación en WebSocket (handshake HTTP)

Las conexiones WebSocket pasan primero por el pipeline HTTP. Usa el handler de auth
para rechazar conexiones no autorizadas en la fase de upgrade:

```java
public class AuthenticatedChatEndpoint implements WebSocketEndpoint {

    private final TokenVerifier verifier;

    public AuthenticatedChatEndpoint(TokenVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    public void onOpen(WebSocketSession session) {
        // El token se pasa como query param en la URL de upgrade:
        // ws://host:8080/chat/general?token=eyJ...
        String token  = session.queryFirst("token");
        var    result = verifier.verify(token, Instant.now());
        if (!result.isValid()) {
            session.close(WebSocketCloseStatus.PROTOCOL_ERROR);
            return;
        }
        session.attribute("userId", result.claims().subject());
        LOG.info("WS autenticado: {}", result.claims().subject());
    }

    @Override
    public void onText(WebSocketSession session, String message) {
        String userId = (String) session.attribute("userId");
        if (userId == null) return; // no autenticado
        // procesar mensaje...
    }

    // ... resto de métodos
}
```

---

## `GlowrootWebSocketEndpointWrapper` — APM para WebSockets

Si usas Glowroot, envuelve tus endpoints para visibilidad de transacciones:

```java
ctx.websocket(WebSocketRoute.of("/chat/{room}",
    new GlowrootWebSocketEndpointWrapper(new ChatEndpoint(), "chat")));
```

---

## Más información

- [Guía ether-websocket-core](ether-websocket-core.md) — contratos `WebSocketEndpoint`, `WebSocketSession`
- [Guía ether-http-jetty12](ether-http-jetty12.md) — `JettyModule` y `JettyServerFactory`
- [Guía ether-glowroot-jetty12](ether-glowroot-jetty12.md) — APM para HTTP y WebSockets
- [Javadoc API](../api/doxygen/html/index.html)
