# CI.md

Integración continua del proyecto.

## Plataforma

- Plataforma de CI: GitHub Actions.
- Archivo de configuración: `.github/workflows/` (validate build, publish, docs).
- Donde ver resultados: pestaña Actions del repo o `make gh-runs`.

## Triggers

| Evento | Pipeline que se ejecuta |
| --- | --- |
| Push a rama principal | `validate-build-on-main.yml` |
| Pull request abierto | `generate-doxygen-docs.yml` (build de docs) |
| Tag push / workflow_dispatch | `publish-java-modules-maven-central.yml` |
| `workflow_run` (success) sobre publish Central | `publish-java-modules-gh-pkg.yml` |

## Gates obligatorios

Estos checks deben pasar antes de mergear cualquier cambio:

| Gate | Herramienta | Comando local |
| --- | --- | --- |
| Build | Maven Wrapper | `make validate-main-build` |
| Collisión vs Central | release plan | `make validate-release-plan` |

## Gates opcionales o informativos

| Gate | Herramienta | Observaciones |
| --- | --- | --- |
| Documentación | Doxygen | `make docs-gen` / `make docs-ci` |

## Política de falla

- Un gate de build rojo bloquea el merge.
- Colisiones de versión en Maven Central bloquean el deploy (no el merge) y se resuelven bump de versión o re-publicación.

## Relación con tareas

Un agente no debe marcar una tarea como `done` si los gates de CI definidos en la
sección de validación de esa tarea no pasan.
