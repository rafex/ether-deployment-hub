+++
doctype = "architecture"
id = "ARCH-0005"
title = "Análisis estático del ecosistema"
status = "active"
created_at = "2026-09-06"
related_decisions = []
related_specs = []
tags = ["analisis", "codebase", "clusters"]
+++

# ARCH-0005 - Análisis estático del ecosistema

## Contexto

Visión estructural del código para guiar documentación y refactors.

## Diseño

532 archivos Java, 2.150 clases, 62 interfaces, 64 routes, 29 scripts Bash, 12 Enums. 12 clusters (Leiden, cohesión 0.63–0.95). Hotspots por fan-in: `TokenValidator.isBlank` (70), `GlowrootJettyHandler.handle` (42), `ClaimsMapper.asText` (38), `ConversationState.add` (35), `ConfigBinder.join` (31).

## Restricciones y consecuencias

Confirma la separación core/entry y las boundaries entre módulos; candidatos a hotspot para optimización o vigilancia; base para documentación de arquitectura.
