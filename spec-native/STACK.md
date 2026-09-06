# STACK.md

Fuente de verdad de la base tecnológica del proyecto.

## Runtime

- Lenguaje: Java
- Versión: JDK 25 LTS (Temurin) — migrado desde Java 21 en la rama `migrate-jdk25`.

## Build

- Maven Wrapper 3.9.12 (compatible con Java 25).
- `ether-parent` como POM padre + BOM: gestiona versiones de dependencias externas (Jackson, Jetty, Glowroot).

## Frameworks / librerías

- Jetty 12 — servidor HTTP y WebSocket sin Servlet (`ether-http-jetty12`, `ether-websocket-jetty12`).
- Jackson — serialización JSON vía `ether-json` (`JsonCodec`).
- Glowroot — APM; compile-api en `0.14.0-beta.3` (última en Central), runtime `>= 0.14.7` desde GitHub Releases.

## Infraestructura

- CI/CD: GitHub Actions; runners con `actions/setup-java@v5` (`distribution: temurin`, `java-version: '25'`).
- Publicación: Maven Central + GitHub Packages (`maven.pkg.github.com`).
- Documentación: Doxygen + Graphviz → GitHub Pages (`docs/api/doxygen/html`).

## Integraciones

- Maven Central — destino de publicación; se valida colisión antes de deploy y publish.
- GitHub Packages — mirror de artefactos firmados con GPG.
- GitHub Releases — distribución de `glowroot.jar` (0.14.7).

## Restricciones

- Glowroot estable (0.14.7) no está en Maven Central (solo ZIP) → runtime desde GitHub Releases.
- JDK 24+ bloquea dynamic agent loading → requiere `-XX:+EnableDynamicAgentLoading`.
- JS actions forzadas a Node 24 via `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24=true`.
- Publicación Central con `central.waitUntil=validated` (configurable).
