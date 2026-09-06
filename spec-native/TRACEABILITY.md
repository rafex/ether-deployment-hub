# TRACEABILITY.md

Mapa de relaciones entre specs, tareas, decisiones y validacion.

## Objetivo

Permitir que una persona o agente pueda reconstruir rapidamente:

- que spec origino un cambio
- que tareas ejecutaron esa spec
- que decisiones condicionaron el trabajo
- que evidencia valida el resultado

## Cuando actualizar este archivo

Actualizar al cerrar una iniciativa, no durante la ejecucion.
El momento correcto es cuando la spec pasa a estado `done` o `blocked`.

Si una decision cambia el alcance de una spec activa, registrar
la relacion antes de continuar.

## Trazabilidad

| Spec | Estado | Tareas | Decisiones | Archivos principales | Validacion | Observaciones |
| --- | --- | --- | --- | --- | --- | --- |
| SPEC-0001 (contexto-docs) | done | TASK-CONTEXTO-DOCS-0001..0005 | DEC-0001..0007 | `spec-native/decisions/`, `architecture/`, `conventions/`, `STACK.md`, `ROADMAP.md` | health-check healthy; list_decisions/list_architecture/list_conventions no vacías | Documentación SpecNative rellenada desde análisis del codebase |
