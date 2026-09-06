+++
doctype = "architecture"
id = "ARCH-0001"
title = "Topología de módulos y orden de despliegue"
status = "active"
created_at = "2026-09-06"
related_decisions = []
related_specs = []
tags = ["modulos", "deploy-order", "dependencias"]
+++

# ARCH-0001 - Topología de módulos y orden de despliegue

## Contexto

29 módulos con dependencias internas; el orden de publicación es crítico para que cada artefacto encuentre sus dependencias en Central.

## Diseño

`releases/manifest.json` define `deployOrder` (29 módulos) y por módulo sus `dependencies` + `internalDependencyProperties`. El análisis del codebase confirma dos capas: core (ether-json, ether-jwt, ether-logging-core, ether-database-core, ether-websocket-core, ether-archetype; fan-in alto, fan-out 0) y entry (ether-brain, ether-di, ether-http-jetty12, ether-glowroot-jetty12).

## Restricciones y consecuencias

Deploy respeta dependencias (ver `deployOrder`); el manifest es la fuente de verdad; boundaries del grafo (ether-brain→ether-jwt 101 calls, ether-brain→ether-logging-core 48, ether-database-sqlite→ether-database-core 14) confirman la topología.
