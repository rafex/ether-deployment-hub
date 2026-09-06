# SPEC.md

```toml
artifact_type = "spec"
id            = "SPEC-0001"
state         = "done"
owner         = "rafex"
created_at    = "2026-09-06"
updated_at    = "2026-09-06"
replaces      = "none"
related_tasks = []
related_decisions = []
```

## Resumen

Rellenar la documentación SpecNative del repositorio con artefactos canónicos
(de decisiones, arquitectura y convenciones) y contexto real extraído del
análisis del codebase (knowledge graph) y de las fuentes del repo
(`PLAN.md`, `AGENTS.md`, `docs/`, `releases/manifest.json`).

## Problema

Tras instalar SpecNative v0.9.0, los directorios `spec-native/decisions/`,
`spec-native/architecture/` y `spec-native/conventions/` quedaron vacíos y las
decisiones históricas del proyecto siguen dispersas en `PLAN.md`, `AGENTS.md` y
`docs/`. El contrato (`SCHEMA.md`) exige artefactos canónicos `DEC-*`, `ARCH-*`
y `CONV-*` con índices derivados.

## Objetivo

Documentación SpecNative completa y fiel al estado real del repo: 7 decisiones,
5 artefactos de arquitectura, 3 convenciones, índices regenerados, y core docs
(`STACK.md`, `ROADMAP.md`) actualizados con datos verificados.

## Alcance

- Incluye: artefactos canónicos + índices derivados, actualización de `STACK.md`
  y `ROADMAP.md`, trazabilidad y validación.
- Excluye: cambiar código de los módulos o del pipeline; solo documentación.

## Criterios de aceptación

- Existen ≥ 1 `DEC-*`, ≥ 1 `ARCH-*` y ≥ 1 `CONV-*` con front matter válido.
- `DECISIONS.md`, `ARCHITECTURE.md` y `CONVENTIONS.md` contienen sus tablas regeneradas.
- `STACK.md` refleja versiones reales (Jackson 2.21.2, Jetty 12.1.7).
- `ROADMAP.md` integra el plan de la auditoría de automatización.
- `health-check` del framework reporta `healthy` sin issues.

## Dependencias y riesgos

- El conocimiento se extrae del knowledge graph del codebase y de docs existentes;
  verificar cifras contra `releases/manifest.json` y `ether-parent/pom.xml`.
- Los índices se regeneran vía las herramientas canónicas (no se editan a mano).

## Plan de ejecución

1. Crear decisiones canónicas (7).
2. Crear artefactos de arquitectura (5).
3. Crear convenciones (3).
4. Actualizar `STACK.md` y `ROADMAP.md`.
5. Trazabilidad, validación y entrega.

## Plan de validación

- `specnative_project` health-check y `status()`.
- `list_decisions()`, `list_architecture()`, `list_conventions()` no vacías.
