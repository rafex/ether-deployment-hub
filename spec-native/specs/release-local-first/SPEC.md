# SPEC.md

```toml
artifact_type = "spec"
id            = "SPEC-0002"
state         = "done"
owner         = "rafex"
created_at    = "2026-09-06"
updated_at    = "2026-09-06"
replaces      = "none"
related_tasks = []
related_decisions = ["DEC-0001", "DEC-0002", "DEC-0003", "DEC-0004"]
```

## Resumen

Habilitar el release de componentes **desde local** (sin depender de GitHub
Actions) y corregir la rotura actual del build de validación de main. Se
aplican las reglas de Ether Best Practices (build-tooling y CD portable):
Makefile = construcción, Justfile = operativas, helpers (`scripts/`) = lógica
real, con secretos cifrados vía sops+age.

## Problema

1. La orquestación del publish (niveles, re-validación, apply, update-manifest)
   vive en los workflows de GitHub Actions. No hay forma de ejecutar un release
   completo desde local; para publicar hay que disparar CI.
2. `Validate Java Build On Main` está roto: `install-all-modules.sh` usa un
   array de orden hardcodeado que instala `ether-http-jetty12` antes que su
   dependencia `ether-websocket-jetty12`, provocando
   `Could not find artifact ... ether-websocket-jetty12:9.5.5-SNAPSHOT`; además
   el array lista 27 módulos en vez de los 29 del manifest (faltan `ether-cron`
   y `ether-websocket-proxy-jetty12`).
3. El publish en CI tarda ~181 min por la espera serializada de publicación
   (`CENTRAL_WAIT_UNTIL=published` por módulo) — el release local con `~/.m2`
   único elimina esa espera entre niveles.

## Objetivo

- Release completo reproducible desde local: `just release` (solo Maven Central).
- Build de validación de main en verde (`make validate-main-build`).
- Secretos cifrados sops+age (`~/.age/ether-deployment-hub-key.txt`), nunca
  hardcodeados.
- Endurecimiento de seguridad del deploy a GH Packages (settings.xml `chmod 600`
  + trap, passphrase vía settings.xml).

## Alcance

- Incluye: fix `install-all-modules.sh`, secretos sops+age, orquestador
  `scripts/deploy-release.sh`, fachadas Make/Just (`release.mk`, `Justfile`),
  endurecimiento de `deploy-to-github-packages.sh`.
- Excluye: rediseño del pipeline CI (paralelización matrix, wrapper de los
  workflows) y mirror a GH Packages en el flujo local `just release`.

## Criterios de aceptación

- `make validate-main-build` verde en local y en CI (main).
- `just env maven` descifra y exporta sin imprimir secretos.
- `just publish-central --dry-run` genera plan y valida colisiones sin deploy.
- `just release` publica a Maven Central (niveles por dependencias, manejo
  already-exists/being-published) y verifica publicación final.
- `deploy-to-github-packages.sh` no expone passphrase en CLI y protege settings.xml.

## Dependencias y riesgos

- `sops`, `age`, `just`, `gh` ya instalados localmente.
- Sonatype puede responder "currently being published" — manejado por
  `wait-for-maven-central-artifact.sh`.
- La clave GPG debe estar disponible (keyring local o `GPG_PRIVATE_KEY_B64`).
- El fix de `install-all-modules.sh` toca un script crítico: validar con
  `make validate-main-build` antes de mergear.

## Plan de ejecución

1. Fix `install-all-modules.sh` (orden derivado del topo-sort canónico).
2. Secretos sops+age.
3. Orquestador local `deploy-release.sh`.
4. Fachadas `release.mk` + `Justfile`.
5. Endurecimiento `deploy-to-github-packages.sh`.
6. Validación y documentación.

## Plan de validación

- `make validate-main-build` (verde).
- `just env maven` (descifrado sin leak).
- `just publish-central --dry-run` (plan + validación).
- Release real de humo (1-2 módulos) con confirmación.
