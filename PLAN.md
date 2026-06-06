# Migracion de Git Submodules a Git Subtree

## Objetivo

Migrar `ether-deployment-hub` de submodules Git a subtrees Git sin perder codigo, trazabilidad de origen, ni confiabilidad en el pipeline de release/publicacion.

La estrategia recomendada es importar snapshots con `git subtree --squash` y mantener la historia completa en los repositorios originales. El hub queda como repositorio operativo para build, release planning, Maven Central y GitHub Packages, con un manifiesto de trazabilidad que registra el repo/SHA exacto importado para cada modulo.

## Estado actual

- El repositorio fue migrado a 27 subtrees importados con `git subtree add --squash`.
- `.gitmodules` fue eliminado.
- No quedan gitlinks `160000` para modulos.
- Los 27 SHAs de origen fueron validados contra sus remotos antes y despues de crear `releases/subtrees.json`.
- `releases/manifest.json` gestiona 26 modulos de release.
- `ether-database-sqlite` existe como subtree, pero no esta en `releases/manifest.json`.
- El release planner ya soporta paths tipo subtree porque detecta cambios por `path` y `path/...`.
- El deploy usa `validate-source-refs`, que valida `releases/subtrees.json`.

## Principios de no perdida

1. No eliminar `.gitmodules` ni gitlinks hasta tener trazabilidad completa en `releases/subtrees.json`. Hecho.
2. Validar que cada SHA grabado en `HEAD` exista en el remoto antes de importar.
3. Importar cada modulo al mismo path que usa hoy el submodule.
4. Registrar para cada modulo:
   - nombre
   - path/prefix
   - remote URL
   - branch
   - imported SHA
   - modo de importacion
5. Ejecutar verificacion de build/release antes de mergear a `main`.
6. Mantener los repositorios originales disponibles como fuente canonica historica.

## Decision recomendada

Usar `git subtree add --squash`.

Ventajas:
- Evita inflar el historial del hub con 27 historiales completos.
- Mantiene checkout simple, sin `git submodule update`.
- Evita fallas de CI por submodules no inicializados.
- Preserva trazabilidad mediante `releases/subtrees.json`.

Tradeoff:
- `git log` del hub no incluira toda la historia interna de cada modulo.
- Para auditoria historica completa se consulta el repositorio original usando el SHA registrado.

## Fases

### Fase 0: Preparacion

- Crear rama `codex/migrate-submodules-to-subtrees`. Hecho.
- Crear `releases/subtrees.json`. Hecho.
- Agregar validacion de referencias subtree. Hecho.
- Identificar referencias a submodules en Makefiles, scripts, workflows y docs. Hecho.
- Decidir si `ether-database-sqlite` entra al manifest de releases o queda documentado como modulo no desplegable.

### Fase 1: Migracion mecanica

Por cada modulo:

```bash
git rm --cached <module>
rm -rf <module>
git subtree add --prefix=<module> <remote-url> main --squash
```

Estado: hecho. Despues de importar todos:

```bash
git rm .gitmodules
git add releases/subtrees.json
```

La copia historica de `.gitmodules` quedo archivada en `docs/archive/gitmodules.pre-subtree-migration`.

### Fase 2: Automatizacion

- Quitar `submodules: recursive` de workflows que ya no lo necesitan. Hecho.
- Reemplazar comandos `submodules-*` por equivalentes `subtrees-*`. Hecho.
- Convertir `validate-submodule-refs` en una validacion de fuentes compatible con subtree. Hecho.
- Reemplazar documentacion y scripts legacy de submodules. Hecho.

### Fase 3: Verificacion local

Ejecutar:

```bash
./scripts/validate-subtree-source-refs.sh
make validate-main-build
make release-plan
```

Si aplica:

```bash
make publish-plan-ci
```

### Fase 4: Verificacion CI

- Confirmar que `Generate Release Plan` detecta cambios dentro de `ether-x/...`.
- Confirmar que `Validate Java Build On Main` ya no requiere checkout recursivo.
- Confirmar que `Publish Java Modules - Maven Central` aplica versiones y despliega por `path` normal.
- Confirmar que `Publish Java Modules - GitHub Packages` sigue usando artefactos de Maven Central run, no polling.

## Criterios de aceptacion

- `git ls-tree HEAD` ya no muestra entradas `160000` para modulos.
- `.gitmodules` no existe o no contiene submodules activos.
- Los 27 modulos existen como directorios normales.
- `releases/subtrees.json` contiene 27 entradas con SHAs exactos.
- Los 26 modulos del manifest siguen resolviendo `pomPath`.
- `ether-database-sqlite` tiene decision explicita.
- `make validate-main-build` pasa.
- `make release-plan` pasa.
- Workflows no dependen de checkout recursivo de submodules.

## Riesgos

- Repo mas grande despues de importar snapshots.
- Merge inicial voluminoso.
- Si se usa `--squash`, el historial fino queda fuera del hub y se consulta en los repos originales.
- Si un modulo tenia cambios locales no pusheados dentro del submodule antes de importar, se perderian en la importacion. Mitigacion aplicada: se importaron los SHAs remotos registrados y validados.
- `ether-database-sqlite` puede quedar fuera de releases si no se agrega al manifest.

## Comandos de sincronizacion futura

Actualizar un subtree desde su repo original:

```bash
git subtree pull --prefix=<module> <remote-url> main --squash
```

Publicar cambios del hub hacia repo original, si se decide mantener flujo bidireccional:

```bash
git subtree push --prefix=<module> <remote-url> main
```

El flujo bidireccional debe usarse con disciplina; para reducir riesgo, el flujo recomendado inicialmente es pull-only desde repos originales hacia el hub.
