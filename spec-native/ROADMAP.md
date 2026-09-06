# ROADMAP.md

Dirección del proyecto en el tiempo.

## Ahora

- Documentación SpecNative: artefactos canónicos (DEC/ARCH/CONV) y contexto real
  del codebase — iniciativa `contexto-docs` (SPEC-0001).
- Migración a JDK 25 LTS (branch `migrate-jdk25`); Javadoc del ecosistema al 100%.

## Después

- Estabilización del pipeline de automatización (plan de la auditoría
  `docs/automation-audit-summary.md`, scoring 5.9/10):
  - Revisar los scripts críticos: `generate-release-plan.sh`,
    `apply-release-plan.sh`, `validate-release-plan-against-central.sh`.
  - Logging estructurado (JSON) y métricas de ejecución.
  - Tests unitarios para `release-common.sh` y smoke tests por script.
  - Retries con backoff y timeouts configurables en operaciones de red.
- Aplicar preview features de Java 25 al estabilizarse: JEP 505 (Structured
  Concurrency) en `ether-ai-*`, JEP 506 (Scoped Values) en contexto HTTP,
  JEP 488 (patterns primitivos) en config/json.

## Más adelante

- Monitoreo del pipeline (dashboard de estado, alertas, métricas históricas).
- Ampliar el stack de documentación (fase 1 Doxygen completada; evaluar fases siguientes).

## No hacer por ahora

- Introducir frameworks pesados (Spring/Quarkus/Micronaut) — contradice los principios de diseño del ecosistema.
