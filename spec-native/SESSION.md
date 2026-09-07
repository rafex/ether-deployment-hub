+++
[session]
state = "in_progress"
agent = "opencode"
initiative = "ci-containers"
task = "build remoto via conexion podman a Debian"
intent = "Compilar/releasear usando el daemon podman remoto (Debian 13), sin copiar el repo al servidor (build embebido por contexto tar). Remoto por defecto."
last_updated = "2026-09-07"
+++

# Active Session

## Current state

Iniciativa `ci-containers` (SPEC-0003): pipeline en contenedor (podman→docker).
Rama `feature/ci-containers` con 3 worktrees ya integrados (imagen, helper,
fachadas, workflow ghcr). Bugs de integracion corregidos (set -u, Containerfile
100644, just release sin re-descifrar). Machine podman local (libkrun) eliminada
a peticion del usuario; el runtime por defecto es un Debian 13 remoto
(`debian-server-wifi`). El bind mount local (`-v /Users/...`) no funciona con
daemon remoto (los bind mounts son locales al daemon). `make ci` end-to-end
quedo sin validar.

## Next steps

1. Redisenar `scripts/container.sh` para build embebido remoto (contexto por tar,
   COPY en Containerfile): `make ci` = `podman build` remoto que compila.
2. Resolver cache Maven en build embebido (cache mounts o multi-stage
   POMs+go-offline).
3. Definir como corre el release/CD sin repo persistente en el servidor
   (secretos via `-e KEY="$VAL"`, cambios de POMs/manifest extraibles).
4. Cerrar SPEC-0003: DEC-0009, TASKS done, COMMANDS/README, ROADMAP+IDEAS.
5. Push `feature/ci-containers` + PR a main + cleanup de worktrees.

## Context for next agent

- Detalle completo en `spec-native/specs/ci-containers/README.md`.
- Decision clave: "Debian no necesita el repositorio" → build embebido, NO rsync.
- `-e KEY` sin valor con daemon remoto lee el env del DAEMON → usar
  `-e KEY="$VAL"` explicito.
- Debian (192.168.3.143): podman 5.4.2, rsync, java, git, jq, ~/.m2 (101 MB),
  125 GB libres, SSH BatchMode OK.
- `docker` es symlink a podman. Sin runtime local.
- Untracked no incluidos en commits: `agents/` (DIAGNOSE.md viejo) y
  `ether-cron/ether-cron/.flattened-pom.xml` (artefacto de build).
