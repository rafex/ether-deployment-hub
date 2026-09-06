# CD.md

Entrega continua del proyecto.

## Plataforma

- Plataforma de CD: GitHub Actions (publish a Maven Central y GitHub Packages).
- Archivo de configuración: `publish-java-modules-maven-central.yml` y `publish-java-modules-gh-pkg.yml`.
- Donde ver el estado de los deploys: `make gh-watch RUN_ID=<id>` / `make gh-logs RUN_ID=<id>`.

## Ambientes

| Ambiente | Rama o tag | Deploy automático | Aprobación requerida |
| --- | --- | --- | --- |
| Maven Central | tag / workflow_dispatch | sí | no (validación previa de colisiones) |
| GitHub Packages | workflow_run tras Central | sí | no |

## Proceso de release

1. `make sync-manifest` sincroniza el manifest contra Maven Central.
2. `make release-plan` detecta cambios y calcula bump semántico.
3. `make validate-release-plan` verifica que no colisione en Central.
4. `make publish-ci` dispara el workflow de Maven Central (deploy en niveles).
5. Al finalizar con éxito, `workflow_run` dispara el mirror a GitHub Packages.
6. `update-manifest` actualiza `releases/manifest.json`.

## Gates de promoción

| De | A | Gates requeridos |
| --- | --- | --- |
| release plan | deploy Central | validación de colisiones (pre-deploy y pre-publish) |
| Central | GH Packages | conclusión `success` del workflow Central |

## Variables y secretos

- Secretos gestionados en GitHub (signing GPG, tokens de Maven Central y GH Packages).
- Variables relevantes: `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24=true`, `central.waitUntil`.
- No documentar valores; solo nombres y propósito.

## Rollback

- Un artefacto ya publicado en Central no se retira; se publica una nueva versión con bump.
- Deploys idempotentes: colisiones `already exists` se omiten por módulo.

## Relación con specs y tareas

Antes de considerar una iniciativa entregada, verificar que los artefactos
afectados fueron publicados en el orden correcto y que `manifest.json` quedó
sincronizado.
