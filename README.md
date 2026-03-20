# Ether Central Publishing Hub

![GitHub Workflow Status (build main)](https://img.shields.io/github/actions/workflow/status/rafex/ether-deployment-hub/validate-build-on-main.yml?branch=main)

## Estado en Maven Central

### Tabla de estado (con badge por modulo)

| Modulo | Badge | GroupId | ArtifactId | Desplegado |
|---|---|---|---|---|
| ether-parent | ![ether-parent](https://img.shields.io/maven-central/v/dev.rafex.ether.parent/ether-parent) | dev.rafex.ether.parent | ether-parent | si |
| ether-config | ![ether-config](https://img.shields.io/maven-central/v/dev.rafex.ether.config/ether-config) | dev.rafex.ether.config | ether-config | no |
| ether-database-core | ![ether-database-core](https://img.shields.io/maven-central/v/dev.rafex.ether.database/ether-database-core) | dev.rafex.ether.database | ether-database-core | no |
| ether-jdbc | ![ether-jdbc](https://img.shields.io/maven-central/v/dev.rafex.ether.jdbc/ether-jdbc) | dev.rafex.ether.jdbc | ether-jdbc | no |
| ether-database-postgres | ![ether-database-postgres](https://img.shields.io/maven-central/v/dev.rafex.ether.database/ether-database-postgres) | dev.rafex.ether.database | ether-database-postgres | no |
| ether-json | ![ether-json](https://img.shields.io/maven-central/v/dev.rafex.ether.json/ether-json) | dev.rafex.ether.json | ether-json | si |
| ether-jwt | ![ether-jwt](https://img.shields.io/maven-central/v/dev.rafex.ether.jwt/ether-jwt) | dev.rafex.ether.jwt | ether-jwt | si |
| ether-observability-core | ![ether-observability-core](https://img.shields.io/maven-central/v/dev.rafex.ether.observability/ether-observability-core) | dev.rafex.ether.observability | ether-observability-core | no |
| ether-http-core | ![ether-http-core](https://img.shields.io/maven-central/v/dev.rafex.ether.http/ether-http-core) | dev.rafex.ether.http | ether-http-core | si |
| ether-http-security | ![ether-http-security](https://img.shields.io/maven-central/v/dev.rafex.ether.http/ether-http-security) | dev.rafex.ether.http | ether-http-security | no |
| ether-http-problem | ![ether-http-problem](https://img.shields.io/maven-central/v/dev.rafex.ether.http/ether-http-problem) | dev.rafex.ether.http | ether-http-problem | no |
| ether-http-openapi | ![ether-http-openapi](https://img.shields.io/maven-central/v/dev.rafex.ether.http/ether-http-openapi) | dev.rafex.ether.http | ether-http-openapi | no |
| ether-http-client | ![ether-http-client](https://img.shields.io/maven-central/v/dev.rafex.ether.http/ether-http-client) | dev.rafex.ether.http | ether-http-client | no |
| ether-http-jetty12 | ![ether-http-jetty12](https://img.shields.io/maven-central/v/dev.rafex.ether.http/ether-http-jetty12) | dev.rafex.ether.http | ether-http-jetty12 | si |
| ether-websocket-core | ![ether-websocket-core](https://img.shields.io/maven-central/v/dev.rafex.ether.websocket/ether-websocket-core) | dev.rafex.ether.websocket | ether-websocket-core | si |
| ether-websocket-jetty12 | ![ether-websocket-jetty12](https://img.shields.io/maven-central/v/dev.rafex.ether.websocket/ether-websocket-jetty12) | dev.rafex.ether.websocket | ether-websocket-jetty12 | si |
| ether-webhook | ![ether-webhook](https://img.shields.io/maven-central/v/dev.rafex.ether.webhook/ether-webhook) | dev.rafex.ether.webhook | ether-webhook | no |
| ether-glowroot-jetty12 | ![ether-glowroot-jetty12](https://img.shields.io/maven-central/v/dev.rafex.ether.glowroot/ether-glowroot-jetty12) | dev.rafex.ether.glowroot | ether-glowroot-jetty12 | no |

### JSON de estado

Consulta el archivo [docs/maven-central-status.json](docs/maven-central-status.json).

## Objetivo del Repositorio

Este repositorio actúa como un hub orquestador para la publicación y despliegue automáticos de los módulos de **Ether** en **Maven Central**. Incluye:

- Scripts y plantillas de configuración para generación de artefactos (Javadoc, fuentes, firmas GPG).
- Workflows de GitHub Actions preconfigurados para ejecutar `mvn deploy` usando el plugin `central-publishing-maven-plugin`.
- Gestión centralizada de credenciales y versiones de cada módulo.
- Orquestación del orden de despliegue respetando el grafo de dependencias entre módulos.
- Generación automática de release plans con detección de cambios por submódulo.

## Acerca de la biblioteca Ether

**Ether** es una colección de componentes Java ligeros para construir servicios sin depender de frameworks pesados. Cada módulo es independiente y puede usarse de forma aislada o combinada.

### Módulos disponibles

#### Fundación

- **ether-parent** — POM padre y BOM. Centraliza versiones de dependencias externas para todos los módulos.

#### Configuración

- **ether-config** — Abstracción de configuración. Lee propiedades desde variables de entorno, archivos o sistema sin acoplar el código a la fuente concreta.

#### Acceso a datos

- **ether-database-core** — Interfaces de acceso a datos: `DataSource`, gestión de conexiones y transacciones. Sin implementación concreta.
- **ether-jdbc** — Implementación JDBC de `ether-database-core`. Pool de conexiones y ejecución de queries.
- **ether-database-postgres** — Dialectos y utilidades específicas de PostgreSQL sobre `ether-database-core`.

#### Serialización

- **ether-json** — Interfaz `JsonCodec` para serializar y deserializar JSON. La implementación (Jackson u otra) es intercambiable sin modificar el código de negocio.

#### Seguridad

- **ether-jwt** — Verificación y firma de JSON Web Tokens usando `JsonCodec`.

#### Observabilidad

- **ether-observability-core** — Interfaces `TimingRecorder`, `RequestIdGenerator` y `ProbeCheck`. Contrato de observabilidad sin dependencia de ningún APM concreto.

#### HTTP

- **ether-http-core** — Interfaces `HttpExchange`, `HttpHandler` y `Middleware`. Contrato del pipeline HTTP sin depender de ningún servidor.
- **ether-http-security** — Perfiles de seguridad HTTP: CORS y security headers (`HttpSecurityProfile`).
- **ether-http-problem** — Respuestas de error estandarizadas según RFC 7807 (`application/problem+json`).
- **ether-http-openapi** — Generación de especificación OpenAPI/Swagger a partir de las rutas registradas.
- **ether-http-client** — Cliente HTTP saliente con serialización via `JsonCodec`.
- **ether-http-jetty12** — Implementación completa del servidor HTTP sobre Jetty 12. Incluye `JettyServerFactory`, autenticación JWT y handler de observabilidad.

#### WebSocket

- **ether-websocket-core** — Interfaces `WebSocketEndpoint` y `WebSocketSession`. Contrato WebSocket sin dependencia de servidor.
- **ether-websocket-jetty12** — Implementación WebSocket sobre Jetty 12.

#### Integraciones

- **ether-webhook** — Envío de webhooks salientes con reintentos, firmas HMAC y serialización de payloads.
- **ether-glowroot-jetty12** — Instrumentación Glowroot APM para Jetty 12: transaction naming, response status, usuario autenticado, request ID, supresión de health checks y slow thresholds por ruta.

### Grafo de dependencias

```
ether-parent
  ├── ether-config
  ├── ether-database-core
  │     ├── ether-jdbc
  │     └── ether-database-postgres
  ├── ether-json
  │     ├── ether-jwt
  │     ├── ether-http-problem  (+ ether-http-core)
  │     ├── ether-http-openapi
  │     ├── ether-http-client
  │     │     └── ether-webhook
  │     └── ether-http-jetty12  (+ ether-config, ether-observability-core,
  │                                ether-http-core, ether-http-security,
  │                                ether-http-problem)
  ├── ether-observability-core
  ├── ether-http-core
  ├── ether-http-security
  ├── ether-websocket-core
  │     └── ether-websocket-jetty12
  └── ether-glowroot-jetty12  (+ ether-http-core, ether-http-jetty12,
                                 ether-websocket-core, ether-websocket-jetty12)
```

### Cómo compilar y publicar

1. Instala todos los módulos en el repo Maven local (orden estricto):
   ```bash
   make install-all
   ```

2. Valida compilación equivalente a CI:
   ```bash
   make validate-main-build
   ```

3. Compila un módulo puntual con sus dependencias:
   ```bash
   make compile-ether-http-jetty12
   ```

4. Genera el release plan local:
   ```bash
   make release-plan
   # o forzando todos los módulos desde el primer commit:
   make release-plan BASE_REF=$(git rev-list --max-parents=0 HEAD)
   ```

5. Publica en Maven Central via GitHub Actions:
   ```bash
   # dry-run primero
   make publish-plan-ci

   # deploy real
   make publish-ci
   ```

### Uso en tu proyecto

Añade el parent POM en tu `pom.xml`:

```xml
<parent>
  <groupId>dev.rafex.ether.parent</groupId>
  <artifactId>ether-parent</artifactId>
  <version><!-- version en Maven Central --></version>
  <relativePath/>
</parent>
```

Luego añade los módulos que necesites:

```xml
<!-- Servidor HTTP con Jetty 12 -->
<dependency>
  <groupId>dev.rafex.ether.http</groupId>
  <artifactId>ether-http-jetty12</artifactId>
</dependency>

<!-- WebSocket con Jetty 12 -->
<dependency>
  <groupId>dev.rafex.ether.websocket</groupId>
  <artifactId>ether-websocket-jetty12</artifactId>
</dependency>

<!-- Instrumentación Glowroot APM -->
<dependency>
  <groupId>dev.rafex.ether.glowroot</groupId>
  <artifactId>ether-glowroot-jetty12</artifactId>
</dependency>

<!-- Observabilidad (interfaces, sin APM concreto) -->
<dependency>
  <groupId>dev.rafex.ether.observability</groupId>
  <artifactId>ether-observability-core</artifactId>
</dependency>
```
