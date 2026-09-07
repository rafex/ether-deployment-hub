# SPEC.md

```toml
artifact_type = "spec"
id            = "SPEC-0003"
state         = "active"
owner         = "rafex"
created_at    = "2026-09-06"
updated_at    = "2026-09-06"
replaces      = "none"
related_tasks = []
related_decisions = ["DEC-0008"]
```

## Resumen

Hacer los pipelines de construcción y release **agnósticos al entorno de
ejecución** mediante contenedores (podman preferido → docker). La imagen de
CI/CD se publica en `ghcr.io` y el mismo flujo corre en local, GitHub Actions o
cualquier runner.

## Problema

El build/validación/release dependen del toolchain del host (JDK, Maven, git,
jq, python3, perl, gpg instalados manualmente) o de runners específicos de
GitHub Actions. No hay un entorno reproducible y versionado.

## Objetivo

- Imagen de CI/CD versionada y publicada (`ghcr.io/rafex/ether-deployment-hub/ci`).
- `make ci` / `just ci` ejecutan el build de validación dentro del contenedor.
- `just release` ejecuta el release completo en contenedor (secretos como env en
  runtime, nunca en la imagen).
- Detección de runtime `podman` → `docker` (con warning, regla 08).

## Alcance

- Incluye: `containers/ci/Containerfile`, `scripts/container.sh`,
  `build-helpers/container.mk`, fachadas Just (`ci`, `release`, `release-host`),
  workflow `build-ci-image.yml` (ghcr.io), deuda técnica documentada.
- Excluye: convertir los workflows existentes (validate-build, publish
  Central/GH-Pkg) a wrappers del contenedor (se documenta como deuda).

## Criterios de aceptación

- `make image` construye la imagen; `make ci` pasa `validate-main-build` (29/29) dentro.
- `just release --dry-run` (o equivalente) genera plan sin deploy dentro del contenedor.
- La imagen no contiene secretos; el CD los recibe por env.
- `build-ci-image.yml` publica en ghcr.io (tags `latest` + SHA).
- Deuda técnica de workflows→wrappers registrada en ROADMAP e intake.

## Dependencias y riesgos

- Requiere `podman` o `docker` en local; `just` dentro de la imagen.
- Ownership de archivos: correr con `-u uid:gid` para no crear archivos root-owned.
- Caché Maven: montar `~/.m2` del host (compartida, sin re-descargar).
- Pin de digest de la imagen base para reproducibilidad/seguridad.

## Plan de ejecución

1. Imagen CI/CD + helper `container.sh`.
2. Fachadas `container.mk` + Just.
3. Workflow ghcr.io.
4. Validación y documentación (DEC-0009, COMMANDS, README, ROADMAP/IDEAS).

## Plan de validación

- `make image` + `make ci` (29/29 en contenedor).
- `just release` dry-run en contenedor.
- health-check SpecNative.
