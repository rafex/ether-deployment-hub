# Ejemplos de uso real

Esta sección muestra cómo los módulos de **Ether** se integran en proyectos de producción.
Cada ejemplo está basado en código real, no en fragmentos inventados.

En particular, los ejemplos priorizan patrones prácticos de:

- arranque de servidores HTTP,
- construcción de endpoints,
- seguridad,
- logging,
- trazabilidad y observabilidad.

## Guías por módulo

| Guía | Descripción |
|---|---|
| [ether-parent](ether-parent.md) | BOM y POM raíz — gestión centralizada de versiones para el ecosistema |
| [ether-config](ether-config.md) | EtherConfig, ConfigSource, binding a Records — configuración 12-factor |
| [ether-crypto](ether-crypto.md) | PasswordHasher, PasswordHasherPBKDF2 — hashing PBKDF2 sin dependencias |
| [ether-json](ether-json.md) | JsonCodec, JsonCodecBuilder — serialización JSON sobre Jackson |
| [ether-di](ether-di.md) | Lazy, Closer y Bootstrap — DI explícita sin reflexión ni anotaciones |
| [ether-logging-core](ether-logging-core.md) | EtherLog, LoggingConfigurator, LogLevels — logging sobre JUL sin dependencias |
| [ether-jwt](ether-jwt.md) | TokenIssuer, TokenVerifier, TokenSpec, TokenClaims — emisión y verificación JWT |
| [ether-observability-core](ether-observability-core.md) | ProbeCheck, ProbeAggregator, ProbeReport — health checks liveness/readiness |
| [ether-database-core](ether-database-core.md) | DatabaseClient, SqlQuery, RowMapper — contratos de acceso a base de datos |
| [ether-jdbc](ether-jdbc.md) | JdbcDatabaseClient, SimpleDataSource — implementación JDBC de DatabaseClient |
| [ether-database-postgres](ether-database-postgres.md) | PostgresErrorClassifier — clasificación semántica de errores PostgreSQL |
| [ether-http-core](ether-http-core.md) | HttpExchange, HttpResource, Route, Middleware — contratos del pipeline HTTP |
| [ether-http-security](ether-http-security.md) | HttpSecurityProfile, CORS, headers, rate limiting, IP filtering |
| [ether-http-problem](ether-http-problem.md) | ProblemDetails (RFC 7807), ProblemException — errores HTTP estructurados |
| [ether-http-openapi](ether-http-openapi.md) | OpenApiDocumentBuilder — especificación OpenAPI 3.x programática |
| [ether-http-client](ether-http-client.md) | EtherHttpClient, HttpRequestSpec, HttpResponseSpec — cliente HTTP tipado |
| [ether-http-jetty12](ether-http-jetty12.md) | JettyServerFactory, JettyModule, JettyServerRunner — servidor HTTP Jetty 12 |
| [ether-websocket-core](ether-websocket-core.md) | WebSocketEndpoint, WebSocketSession, WebSocketRoute — contratos WebSocket |
| [ether-websocket-jetty12](ether-websocket-jetty12.md) | Integración WebSocket con Jetty 12 |
| [ether-webhook](ether-webhook.md) | WebhookSigner, WebhookVerifier, WebhookDeliveryClient — webhooks HMAC |
| [ether-glowroot-jetty12](ether-glowroot-jetty12.md) | Middlewares APM Glowroot para Jetty 12 |
| [ether-ai-core](ether-ai-core.md) | AiChatModel, AiMessage, AiChatRequest/Response — contratos GenAI provider-agnostic |
| [ether-ai-openai](ether-ai-openai.md) | OpenAiConfig, OpenAiChatModel — adapter para GPT-4o y Azure OpenAI |
| [ether-ai-deepseek](ether-ai-deepseek.md) | DeepSeekConfig, DeepSeekChatModel — adapter para DeepSeek-Chat y DeepSeek-Reasoner |
| [ether-brain](ether-brain.md) | Runtime de agentes IA — sesiones, tools y loop de razonamiento |
| [ether-hexagonal-archetype](ether-archetype.md) | Arquetipo Maven — proyecto hexagonal completo con Jetty, PostgreSQL y ether-di |

## Proyectos de ejemplo

| Proyecto | Descripción | Módulos usados |
|---|---|---|
| [Kiwi](kiwi.md) | Casos prácticos de servidor Jetty, endpoints, JWT, logging, health checks y APM | ether-config, ether-di, ether-jwt, ether-jdbc, ether-http-jetty12, ether-http-security, ether-glowroot-jetty12 |

---

## Patrón general de una aplicación Ether

Toda aplicación Ether sigue la misma estructura de capas:

```
                  ┌──────────────────────────────┐
                  │        Transport layer        │  ← ether-http-jetty12
                  │   (HTTP, CLI, gRPC, etc.)     │
                  └──────────────┬───────────────┘
                                 │
                  ┌──────────────▼───────────────┐
                  │        Bootstrap / DI         │  ← ether-di
                  │   (KiwiContainer, wiring)     │
                  └──────────────┬───────────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              │                  │                  │
  ┌───────────▼──────┐ ┌────────▼────────┐ ┌───────▼────────┐
  │   Core (domain)  │ │  Ports (APIs)   │ │  Infra (impl)  │
  │  use-cases, etc. │ │  interfaces     │ │  PostgreSQL, …  │
  └──────────────────┘ └─────────────────┘ └────────────────┘
```

El contenedor de dependencias (`Bootstrap`) crea y conecta todos los componentes.
El servidor HTTP recibe el contenedor ya inicializado y registra rutas, auth y middlewares.
