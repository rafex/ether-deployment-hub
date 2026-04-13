# Guía práctica: ether-websocket-core

**ether-websocket-core** define los contratos para WebSockets en Ether: `WebSocketEndpoint`,
`WebSocketSession` y `WebSocketRoute`. Independiente del servidor concreto.

## Instalación

```xml
<dependency>
    <groupId>dev.rafex.ether.websocket.core</groupId>
    <artifactId>ether-websocket-core</artifactId>
    <version>8.0.0-SNAPSHOT</version>
</dependency>
```

---

## `WebSocketEndpoint` — implementar un endpoint

```java
public class ChatEndpoint implements WebSocketEndpoint {

    private static final EtherLog LOG = EtherLog.getLogger(ChatEndpoint.class);

    // Mapa de sesiones activas (thread-safe)
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void onOpen(WebSocketSession session) {
        sessions.put(session.id(), session);
        String room = session.pathParam("room");
        LOG.info("Cliente conectado id={} room={}", session.id(), room);
        session.sendText("Bienvenido a la sala: " + room);
    }

    @Override
    public void onText(WebSocketSession session, String message) {
        LOG.debug("Mensaje de {} : {}", session.id(), message);
        // Broadcast a todos en la misma sala
        String room = session.pathParam("room");
        sessions.values().stream()
            .filter(s -> room.equals(s.pathParam("room")) && s.isOpen())
            .forEach(s -> s.sendText(session.id() + ": " + message));
    }

    @Override
    public void onBinary(WebSocketSession session, ByteBuffer data) {
        // Manejo de mensajes binarios
        session.sendBinary(data);
    }

    @Override
    public void onClose(WebSocketSession session, WebSocketCloseStatus status) {
        sessions.remove(session.id());
        LOG.info("Cliente desconectado id={} code={}", session.id(), status.code());
    }

    @Override
    public void onError(WebSocketSession session, Throwable error) {
        LOG.error("Error en session {}: {}", session.id(), error.getMessage());
        sessions.remove(session.id());
    }

    @Override
    public Set<String> subprotocols() {
        return Set.of("chat"); // subprotocolos soportados (puede ser vacío)
    }
}
```

---

## `WebSocketSession` — operaciones en la conexión

```java
// Identificación
String id          = session.id();        // ID único de sesión
String path        = session.path();      // "/chat/general"
String subprotocol = session.subprotocol(); // "chat" o ""

// Parámetros de path y query
String room    = session.pathParam("room");
String userId  = session.queryFirst("userId");

// Cabeceras de la handshake HTTP
String origin  = session.headerFirst("Origin");

// Estado
boolean open = session.isOpen();

// Atributos de sesión (para datos de usuario autenticado, etc.)
session.attribute("userId", authenticatedUserId);
Long uid = (Long) session.attribute("userId");

// Enviar mensajes (asíncrono — CompletionStage)
CompletionStage<Void> sent = session.sendText("hola");
sent.toCompletableFuture().get(); // esperar si es necesario

session.sendBinary(ByteBuffer.wrap(bytes));

// Cerrar
session.close(WebSocketCloseStatus.NORMAL_CLOSURE);
```

---

## `WebSocketRoute` — asociar patrón de URL a endpoint

```java
WebSocketRoute route1 = WebSocketRoute.of("/chat/{room}", new ChatEndpoint());
WebSocketRoute route2 = WebSocketRoute.of("/notifications", new NotificationsEndpoint());
WebSocketRoute route3 = WebSocketRoute.of("/live/{stream}/data", new StreamEndpoint());
```

---

## `WebSocketCloseStatus` — códigos de cierre

```java
WebSocketCloseStatus.NORMAL_CLOSURE    // 1000 — cierre limpio
WebSocketCloseStatus.GOING_AWAY        // 1001 — el servidor se apaga
WebSocketCloseStatus.PROTOCOL_ERROR    // 1002
WebSocketCloseStatus.UNSUPPORTED_DATA  // 1003
WebSocketCloseStatus.NO_STATUS_RCVD    // 1005
WebSocketCloseStatus.ABNORMAL_CLOSURE  // 1006
```

---

## Más información

- [Guía ether-websocket-jetty12](ether-websocket-jetty12.md) — implementación Jetty 12
- [Javadoc API](../api/doxygen/html/index.html)
