# ROADMAP.md

Dirección del proyecto en el tiempo.

## Ahora

- Integrar SpecNative Development como contexto operativo del repo (esta iniciativa).
- Migración a JDK 25 LTS (branch `migrate-jdk25`).

## Después

- Aplicar preview features de Java 25 cuando se estabilicen: JEP 505 (Structured Concurrency) en `ether-ai-*`, JEP 506 (Scoped Values) en contexto HTTP, JEP 488 (patterns primitivos) en config/json.

## Más adelante

- Ampliar el stack de documentación (fase 1 Doxygen completada; evaluar fases siguientes).

## No hacer por ahora

- Introducir frameworks pesados (Spring/Quarkus/Micronaut) — contradice los principios de diseño del ecosistema.
