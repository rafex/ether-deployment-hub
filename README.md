# Ether Central Publishing Hub

![GitHub Workflow Status (build main)](https://img.shields.io/github/actions/workflow/status/rafex/ether-deployment-hub/validate-build-on-main.yml?branch=main)

## Estado en Maven Central

### Tabla de estado (con badge por modulo)

| Modulo | Badge | GroupId | ArtifactId | Desplegado |
|---|---|---|---|---|
| ether-parent | ![ether-parent](https://img.shields.io/maven-central/v/dev.rafex.ether.parent/ether-parent) | dev.rafex.ether.parent | ether-parent | si |
| ether-config | ![ether-config](https://img.shields.io/maven-central/v/dev.rafex.ether.config/ether-config) | dev.rafex.ether.config | ether-config | no |
| ether-database-core | ![ether-database-core](https://img.shields.io/maven-central/v/dev.rafex.ether.database/ether-database-core) | dev.rafex.ether.database | ether-database-core | no |
| ether-jdbc | ![ether-jdbc](https://img.shields.io/maven-central/v/dev.rafex.ether.jdbc/ether-jdbc) | dev.rafex.ether.jdbc | ether-jdbc | si |
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
| ether-glowroot-jetty12 | ![ether-glowroot-jetty12](https://img.shields.io/maven-central/v/dev.rafex.ether.glowroot/ether-glowroot-jetty12) | dev.rafex.ether.glowroot | ether-glowroot-jetty12 | si |

### JSON de estado

Consulta el archivo [docs/maven-central-status.json](docs/maven-central-status.json).

## Objetivo del Repositorio

Este repositorio actúa como un hub orquestador para la publicación y despliegue automáticos de los módulos de **Ether** actualmente integrados en **Maven Central**. Incluye:
- Scripts y plantillas de configuración para generación de artefactos (Javadoc, fuentes, firmas GPG).
- Workflows de GitHub Actions preconfigurados para ejecutar `mvn deploy` usando el plugin `central-publishing-maven-plugin`.
- Gestión centralizada de credenciales y versiones de cada módulo.
- Ejemplos y documentación paso a paso para asegurar despliegues consistentes, seguros y reproducibles en el repositorio central de Maven.



## Acerca de la biblioteca Ether

Este repositorio orquesta el despliegue de módulos de **Ether**, una colección de componentes Java ligeros para construir servicios sin depender de frameworks pesados.

### Componentes principales

- **ether-parent**: POM padre con configuración común.
- **ether-json**: Validación y manipulación de JSON.
- **ether-jwt**: Autenticación basada en JSON Web Tokens.
- **ether-http-core**: Abstracciones y contratos HTTP base.
- **ether-http-jetty12**: Integración HTTP usando Jetty 12.
- **ether-websocket-core**: Contratos y primitivas WebSocket agnósticas al transporte.
- **ether-websocket-jetty12**: Integración WebSocket usando Jetty 12.

### Cómo compilar y publicar

1. Valida compilación local equivalente a CI:
   ```bash
   make validate-main-build
   ```
2. Compila un módulo puntual:
   ```bash
   make compile-ether-http-core
   ```
3. Publica módulos desde este hub con GitHub Actions (workflow `Publish All Java Modules` por tag).
4. Para usar en tu proyecto:
   ```xml
   <parent>
     <groupId>dev.rafex.ether.parent</groupId>
     <artifactId>ether-parent</artifactId>
     <version><!-- version publicada en Maven Central --></version>
     <relativePath/>
   </parent>
   ```
   Y luego añade el módulo deseado, por ejemplo:
   ```xml
   <dependency>
     <groupId>dev.rafex.ether.http</groupId>
     <artifactId>ether-http-core</artifactId>
     <version><!-- version publicada en Maven Central --></version>
   </dependency>
   ```
