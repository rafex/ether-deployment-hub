# COMMANDS.md

Lista de comandos operativos del proyecto (no comandos del framework).

## Build y validación

```bash
make install-all                 # instala todos los módulos localmente
make validate-main-build         # validación equivalente a CI
make compile-ether-http-jetty12
make compile-ether-glowroot-jetty12
```

## Release plan

```bash
make release-plan                # detecta cambios desde el último commit
FIRST=$(git rev-list --max-parents=0 HEAD)
make release-plan BASE_REF=$FIRST   # fuerza todos los módulos
make validate-release-plan       # verifica que no colisionen en Central
```

## Deploy a Maven Central (vía GitHub Actions)

```bash
make sync-manifest               # sincroniza versiones del manifest con Central
make verify-central              # verifica que los artefactos publicados existen
make deploy                      # secuencia completa: sync + verify + build + plan
make publish-plan-ci             # dry-run (no despliega)
make publish-ci                  # deploy real
make publish-gh-pkg-ci RUN_ID=<maven-central-run-id>   # re-run manual de GH Packages
```

## Observación de CI

```bash
make gh-runs
make gh-watch RUN_ID=<id>
make gh-logs RUN_ID=<id>
```

## Documentación

```bash
make docs-gen
make docs-gen-docker
make docs-gen-local
make docs-ci
make docs-clean
```

## Utilidad

```bash
make help
make new TAG=<version> MESSAGE="msg"
make retag TAG=<version> MESSAGE="msg"
make subtrees-status
make subtrees-pull
make validate-source-refs
make validate-subtree-source-refs
```
