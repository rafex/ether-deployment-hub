+++
doctype = "decision"
id = "DEC-0008"
title = "Release local-first de componentes"
status = "accepted"
created_at = "2026-09-06"
owners = []
related_specs = ["SPEC-0002"]
related_tasks = []
related_architecture = ["ARCH-0002", "ARCH-0003"]
supersedes = []
tags = ["release", "local-first", "cd", "makefile", "justfile", "sops"]
+++

# DEC-0008 - Release local-first de componentes

## Contexto

La orquestación del publish vivía exclusivamente en los workflows de GitHub
Actions (`publish-java-modules-maven-central.yml` + `deploy-one-level.yml`), por
lo que no era posible ejecutar un release completo desde local: para publicar
había que disparar CI. Además, el build de validación de `main` estaba roto por
el orden hardcodeado de `install-all-modules.sh`.

## Decisión

Adoptar un release **local-first** siguiendo las reglas de buenas prácticas
`build-tooling` (Makefile = construcción, Justfile = operativas, helpers =
lógica real) y `cd` (CD portable, reproducible desde local):

- **Makefile** (`build-helpers/release.mk`): `release-build` (sync manifest +
  plan + validación de colisiones) y `release-apply` (aplicar plan a POMs).
- **Justfile** (`build-helpers/just/release.just`): `env maven` (descifra y
  exporta secretos), `publish-central [base_ref] [head_ref]`, `verify-published`,
  `release`.
- **Lógica real** en `scripts/`: `deploy-release.sh` (orquestador por niveles),
  `secrets.sh` (sops+age), más los scripts existentes.
- **Secretos** cifrados con sops+age en `.secrets/secrets.maven.enc.yaml`, con
  clave age privada en `~/.age/ether-deployment-hub-key.txt` (nunca versionada).
- **Orden de instalación** de `install-all-modules.sh` derivado del grafo de
  dependencias (topo-sort), no hardcodeado.
- El release local cubre **solo Maven Central**; el mirror a GitHub Packages
  permanece en CI por ahora.
- Deploy con `CENTRAL_WAIT_UNTIL=validated`; al ser `~/.m2` local compartido,
  los niveles siguientes resuelven dependencias de lo ya instalado sin esperar
  la publicación en Central.

## Consecuencias

- Release completo reproducible desde local (`just release`), sin CI.
- Los workflows de CI quedan como wrappers parametrizadores (conversión futura,
  no en este alcance).
- Requiere `sops`, `age`, `just` y `gh` instalados localmente.
- Los secretos de la aplicación (OSSRH, GPG) viven cifrados en `.secrets/`; los
  de plataforma (GH_TOKEN) siguen en el proveedor.
