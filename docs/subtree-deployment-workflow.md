# Flujo de trabajo con subtrees y despliegue

Este repositorio publica desde el contenido que existe dentro de `ether-deployment-hub`.
Los modulos viven como `git subtree`, por lo que un clone normal ya contiene todo lo
necesario para compilar, planificar releases y desplegar.

## Reglas operativas

- Cambios de CI, release planning, manifests, documentacion global y scripts se quedan en `ether-deployment-hub`.
- Cambios de codigo de un modulo deben sincronizarse tambien con su repositorio fuente.
- Para reducir riesgo, no empujar cambios de subtree directo a `main`; usar una rama y abrir PR en el repo fuente.
- Antes de publicar, el hub debe estar limpio, compilable y con release plan validado.
- No usar `.gitmodules`, `git submodule update` ni `git clone --recurse-submodules`.

## Bajar cambios desde repos fuente

Ver el inventario configurado:

```bash
make subtrees-status
```

Actualizar todos los subtrees desde `releases/subtrees.json`:

```bash
make subtrees-pull
```

Actualizar solo un modulo:

```bash
git subtree pull \
  --prefix=<module> \
  <remote-url> \
  main \
  --squash
```

Despues de bajar cambios, validar y commitear el resultado en el hub:

```bash
make validate-source-refs
make validate-main-build
make release-plan BASE_REF=origin/main HEAD_REF=HEAD
make validate-release-plan
git status
git add <paths>
git commit -m "chore: sync <module> subtree"
```

## Subir cambios de un modulo a su repo fuente

Si se modifico codigo dentro de un prefijo de modulo, por ejemplo `ether-json/`,
generar una rama en el repo fuente usando `git subtree push`:

```bash
git subtree push \
  --prefix=ether-json \
  git@github.com:rafex/ether-json.git \
  codex/sync-from-deployment-hub
```

Luego abrir PR en `rafex/ether-json` desde `codex/sync-from-deployment-hub` hacia
`main`. Cuando el PR del repo fuente se mergea, regresar al hub y sincronizar:

```bash
make subtrees-pull
make validate-main-build
git add ether-json
git commit -m "chore: sync ether-json subtree"
```

Este paso evita que el hub publique codigo que no existe en el repositorio fuente
del modulo.

## Preparar cambios para despliegue desde el hub

Antes de disparar CI, revisar que no haya referencias antiguas ni cambios sin
commitear:

```bash
git status --short
git ls-tree -r HEAD | awk '$1 == "160000" { print }'
find . -path ./.git -prune -o -name .gitmodules -print
rg -n "git submodule|submodule update|--recurse-submodules|\\.gitmodules" \
  . --glob '!PLAN.md' --glob '!docs/archive/**' --glob '!.git/**'
```

Validar build y release plan local:

```bash
make validate-source-refs
make validate-main-build
make release-plan BASE_REF=origin/main HEAD_REF=HEAD
make validate-release-plan
```

Subir la rama del hub y abrir PR hacia `main`:

```bash
git push origin HEAD
```

El CI de validacion debe pasar en el PR antes de mergear.

## Desplegar a Maven Central y GitHub Packages

Despues de mergear el PR del hub en `main`, actualizar localmente:

```bash
git checkout main
git pull --ff-only origin main
```

Hacer un dry-run para confirmar que el plan selecciona los modulos esperados:

```bash
make publish-plan-ci
make gh-runs
```

Si el plan es correcto, lanzar el despliegue real a Maven Central:

```bash
make publish-ci
make gh-runs
```

La publicacion a GitHub Packages se dispara automaticamente cuando el workflow de
Maven Central termina correctamente. Para reintentar GitHub Packages de forma
manual, usar el run id de Maven Central:

```bash
make publish-gh-pkg-ci RUN_ID=<maven-central-run-id>
```

## Despliegue forzado

Para forzar que el release plan considere todo el historial del hub:

```bash
FIRST=$(git rev-list --max-parents=0 HEAD)
make publish-plan-ci BASE_REF=$FIRST
make publish-ci BASE_REF=$FIRST
```

Usar este modo solo cuando se quiera replanificar todos los modulos; el pipeline
saltara artifacts ya publicados si Maven Central reporta colision existente.

