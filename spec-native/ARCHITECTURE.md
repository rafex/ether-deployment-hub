# ARCHITECTURE.md

Índice de arquitectura. Los componentes, límites y flujos canónicos viven en
`architecture/ARCH-XXXX-<slug>.md`.

## Descripción general

`ether-deployment-hub` es un hub orquestador (no una aplicación) que compila y
publica los módulos del ecosistema **Ether** en Maven Central y GitHub Packages.

## Componentes

| Componente | Responsabilidad |
| --- | --- |
| `releases/manifest.json` | Fuente de verdad: módulos, versiones, `deployOrder`, `releasePolicy` |
| `releases/subtrees.json` | Referencias de subtrees y SHAs de fuente |
| `scripts/` | Release planning, `compute-deploy-levels.sh`, deploy, sync vs Central, validación de refs |
| `.github/workflows/` | Pipelines de publish (Maven Central + GH Packages), validate build y docs |
| `build-helpers/*.mk` | Make targets: git, compile, gh, docs |
| `ether-*/` | Código fuente de los módulos (subtrees, `projectDir` definido en manifest) |

## Deploy order

Controlado por `releases/manifest.json` (`deployOrder`) y aplicado en el release
planning. Ver `AGENTS.md` "Current deploy order" para el orden base (7 módulos
core) y el manifest para la lista completa de 29.

## Publish pipeline (split)

1. **`publish-java-modules-maven-central.yml`** — trigger por tag push o
   `workflow_dispatch`; jobs `release-plan` → `deploy-level-0..6` →
   `update-manifest`. Recolecta JARs/POMs en `~/.m2` y los sube como artefactos
   `maven-artifacts-level-N`.
2. **`publish-java-modules-gh-pkg.yml`** — trigger por `workflow_run` (success)
   sobre el anterior; reusa `release-plan` y `maven-artifacts-level-N` sin volver
   a publicar en Central, firmando y desplegando a `maven.pkg.github.com`.

## Level chunking

`compute-deploy-levels.sh` usa topological sort (Kahn) + `MAX_LEVEL_SIZE=5`
para evitar timeouts de 30 min en GitHub Actions. Ver `AGENTS.md` "Deployment
level chunking" para el desglose L0..L7.

## Límites y dependencias

- Cada módulo `ether-*` es un subtree con su propio `pom.xml`; las dependencias
  internas y sus properties de versión están en `manifest.json` (`dependencies`,
  `internalDependencyProperties`).
- El hub no decide el contenido de cada módulo; solo el orden y la validación de
  publicación.
