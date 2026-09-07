# ci-containers — Pipeline en contenedor (podman/docker)

Análisis, avance y decisión de la iniciativa **SPEC-0003**. Este archivo es el
punto de retoma para el siguiente agente.

## Objetivo

Hacer los pipelines de build/validación/release **agnósticos al entorno**
mediante contenedores (podman preferido → docker), con la imagen publicada en
`ghcr.io`. El mismo flujo debe correr en local, GitHub Actions o cualquier
runner.

## Estado actual (completado)

Rama: `feature/ci-containers` (local, commits integrados de 3 worktrees).

| Componente | Archivo | Estado |
|---|---|---|
| Imagen CI/CD | `containers/ci/Containerfile` | base `docker.io/library/eclipse-temurin:25-jdk-noble@sha256:955c8b…` + git/jq/python3/perl/make/gnupg/maven/just. Sin secretos. |
| Helper runtime | `scripts/container.sh` | acciones `runtime\|image\|image-pull\|ci\|run`; podman→docker (warning); `-u uid:gid`; `~/.m2`→`/var/maven/.m2`; `GNUPGHOME`; passthrough secretos. |
| Fachadas Make | `build-helpers/container.mk` | `make runtime\|image\|image-pull\|ci`. |
| Fachadas Just | `build-helpers/just/release.just` | `just ci`, `just release` (contenedor), `just release-host`. |
| Workflow ghcr | `.github/workflows/build-ci-image.yml` | push a `ghcr.io/rafex/ether-deployment-hub/ci` (`latest`+SHA), triggers: `containers/**`, dispatch, cron semanal. |

Bugs corregidos durante la integración:
- `set -u` con arrays vacíos (`"${env_args[@]}"` en `container.sh`).
- `Containerfile` modo 100644 (estaba 100755).
- `just release` re-descifraba secretos dentro del contenedor (sops/age no están
  en la imagen) → ahora `publish-and-verify` sin `env maven` dentro.
- Base de imagen con nombre completo (`docker.io/library/…`) + pin de digest.

Limpieza de entorno: podman machine local (`libkrun`) y connection
`podman-machine-default-root` eliminadas (se usa podman remoto).

## Validación realizada

- `make image`: OK (la imagen construye; `podman build` funciona contra el
  daemon remoto porque envía el contexto por tar).
- `bash -n scripts/container.sh`, `just --fmt --check`, `just --list`,
  `make -n ci`: OK.
- Toolchain de la imagen verificado vía `podman run` remoto sin volumen
  (java, maven, git, jq, just presentes).
- `make ci` (bind mount del workspace): **NO ejecutado** — ver por qué abajo.

## Por qué falla el bind mount con el daemon Debian

- El podman por defecto apunta a un **Debian 13 remoto**
  (`debian-server-wifi` → `ssh://rafex@192.168.3.143/…/podman.sock`).
- `podman run -v /Users/…:/workspace` falla con `statfs …: no such file or
  directory`: los **bind mounts son locales al daemon**; el daemon del Debian no
  conoce los paths de macOS. No es configurable (ni en podman ni en docker).
- `podman build` **sí** funciona remoto (contexto por tar). `podman run` sin
  volumen también.
- El Debian tiene: podman 5.4.2, rsync, java, git, jq, `~/.m2` (101 MB), 125 GB
  libres. SSH BatchMode OK.

## Decisión tomada (del usuario)

1. Compilar usando la conexión podman a Debian (**remoto por defecto**).
2. **"Debian no necesita el repositorio"**: NO sincronizar el repo al servidor
   como directorio persistente. El código viaja dentro del **contexto del build
   (tar)** — enfoque *build embebido*.
3. `make ci` / `just release` usan la conexión activa (remoto por defecto).

## Implicación técnica (a resolver por el siguiente agente)

- Enfoque **build embebido**: un `Containerfile` de build hace `COPY` del
  contexto y compila dentro; el resultado de `make ci` = exit code del
  `podman build` remoto. El código **no** queda como repo persistente en el
  servidor.
- Retos abiertos:
  1. **Caché Maven** en build embebido: el `~/.m2` no persiste entre builds
     salvo *build cache mounts* (BuildKit/podman) o multi-stage con POMs
     primero + `dependency:go-offline` (patrón ya usado en
     `ether-brain/Dockerfile`). Definir cómo cachear contra el daemon remoto
     (`~/.m2` del servidor como volumen vs cache mounts).
  2. **Release/CD con secretos**: los secretos no deben ir en capas.
     `apply-release-plan.sh` muta POMs/`manifest.json` del working tree; con
     build embebido esos cambios viven en la imagen, no en el host. Decidir:
     (a) release sigue en host, (b) extraer artefactos/cambios con `podman cp`,
     (c) volumen remoto (contradice "no repo en servidor").
  3. **`-e KEY` sin valor** con daemon remoto lee el entorno del **daemon**, no
     del cliente → pasar `-e KEY="$VAL"` explícito (viaja cifrado por SSH).

## Próximos pasos (ordenados)

1. Rediseñar `container.sh` para **build embebido remoto** (o `Containerfile.build`
   + `make ci` = `podman build` remoto con target de compilación).
2. Resolver la caché Maven del build embebido.
3. Definir cómo corre el release (CD) sin repo en el servidor.
4. Cerrar SPEC-0003: DEC-0009, TASKS → done, COMMANDS/README, ROADMAP + IDEAS
   (deuda técnica: workflows→wrappers).
5. `git push` de `feature/ci-containers` + PR a main + cleanup de worktrees.

## Ramas/worktrees pendientes de limpiar

- Worktrees: `task-2026-09-06-ci-image`, `-ci-facades`, `-ci-ghcr`.
- Branches ya mergeadas: `feature/ci-containers-image/facades/ghcr`.

## Notas de entorno

- Podman conexión default: `debian-server-wifi` (192.168.3.143).
- `docker` es un symlink a podman (`/usr/local/bin/docker → /opt/homebrew/bin/podman`).
- Sin runtime local (machine borrada a petición del usuario).
