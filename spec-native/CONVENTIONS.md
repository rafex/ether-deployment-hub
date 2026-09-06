# CONVENTIONS.md

Índice de reglas operativas y de implementación. Las convenciones canónicas
viven en `conventions/CONV-XXXX-<slug>.md`.

## Commits

- Conventional Commits (ejemplos del historial: `perf(tools-remote):`, `docs(brain):`, `tmp:`).
- `releasePolicy` en `manifest.json`: `conventionalCommits: true`,
  `dependencyPropagation: patch`, `docsOnly: none`, `testsOnly: none`,
  `buildOnly: patch`.

## Versionado y release

- Versionado independiente por módulo (`versioningStrategy: independent`).
- Bump semántico automático por detección de cambios (change detection + semver).
- `releases/manifest.json` es la fuente de verdad; sincronizar contra Maven Central antes de planear releases.

## CI/runtime

- JS actions forzadas a Node 24 (`FORCE_JAVASCRIPT_ACTIONS_TO_NODE24=true`).
- Publicación Central con `central.waitUntil=validated`.
- GPG signing con paralelismo acotado (`MAX_GH_PARALLEL=4` en `deploy-to-github-packages.sh`).

## Java 25

- Mejoras JEP 441/444/456 ya aplicadas en 5 módulos (ver `AGENTS.md`).
- Pendientes (preview, al estabilizarse): JEP 505, 506, 488.

## Estructura

- Cada módulo `ether-*` es un subtree con su propio `pom.xml` (`projectDir` en manifest).
- Separación Makefile (build) vs comandos de operación; documentar solo los reales en `COMMANDS.md`.
