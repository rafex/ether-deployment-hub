# TASKS.md

```toml
artifact_type = "task_file"
initiative = "release-local-first"
spec_id = "SPEC-0002"
owner = "rafex"
state = "done"
```

## Tareas

### TASK-RELEASE-LOCAL-0001 - Fix orden de install-all-modules.sh

```toml
id = "TASK-RELEASE-LOCAL-0001"
title = "Fix orden de install-all-modules.sh"
state = "done"
priority = "p1"
owner = "rafex"
labels = []
dependencies = []
expected_files = ["scripts/install-all-modules.sh"]
close_criteria = "El orden de instalación se deriva del topo-sort canónico (29 módulos) y make validate-main-build pasa."
validation = ["make validate-main-build"]
completion_evidence = ["make validate-main-build: 29/29 modules installed, 0 errors"]
```

Eliminar el array hardcodeado de 27 módulos; derivar el orden de
`compute-deploy-levels.sh`/manifest. Añadir `ether-cron` y
`ether-websocket-proxy-jetty12`. `set -euo pipefail`, mktemp+trap.

### TASK-RELEASE-LOCAL-0002 - Secretos sops+age

```toml
id = "TASK-RELEASE-LOCAL-0002"
title = "Secretos sops+age"
state = "done"
priority = "p1"
owner = "rafex"
labels = []
dependencies = []
expected_files = [".sops.yaml", ".secrets/secrets.maven.enc.yaml", "scripts/secrets.sh"]
close_criteria = "just/env descifran OSSRH_* y GPG sin leak; clave age en ~/.age/ether-deployment-hub-key.txt."
validation = ["scripts/secrets.sh --action verify"]
completion_evidence = ["scripts/secrets.sh --action env --show-values emite 4 exports; sops --decrypt OK"]
```

Clave age en `~/.age/ether-deployment-hub-key.txt`; `.sops.yaml` con recipients;
`.secrets/secrets.maven.enc.yaml` con OSSRH_USERNAME, OSSRH_PASSWORD,
MAVEN_GPG_PASSPHRASE, GPG_PRIVATE_KEY_B64.

### TASK-RELEASE-LOCAL-0003 - Orquestador local deploy-release.sh

```toml
id = "TASK-RELEASE-LOCAL-0003"
title = "Orquestador local deploy-release.sh"
state = "done"
priority = "p1"
owner = "rafex"
labels = []
dependencies = ["TASK-RELEASE-LOCAL-0001"]
expected_files = ["scripts/deploy-release.sh"]
close_criteria = "Ejecuta plan→validate→apply→niveles→make deploy→update-manifest con --dry-run funcional."
validation = ["scripts/deploy-release.sh --dry-run"]
completion_evidence = ["bash -n OK; --dry-run genera plan y validación (selectedCount 0) sin deploy"]
```

Porta la lógica de deploy-one-level.yml. Flags --base-ref/--head-ref/--dry-run/
--levels/--skip-tests/--log-file. `CENTRAL_WAIT_UNTIL=validated`.

### TASK-RELEASE-LOCAL-0004 - Fachadas release.mk + Justfile

```toml
id = "TASK-RELEASE-LOCAL-0004"
title = "Fachadas release.mk + Justfile"
state = "done"
priority = "p1"
owner = "rafex"
labels = []
dependencies = ["TASK-RELEASE-LOCAL-0002", "TASK-RELEASE-LOCAL-0003"]
expected_files = ["build-helpers/release.mk", "Justfile", "build-helpers/just/release.just"]
close_criteria = "make release-build y just env maven/publish-central/verify-published/release existen y delegan."
validation = ["make release-build --dry-run", "just --list"]
completion_evidence = ["just --list muestra 4 recipes; eval \"$(just env maven)\" exporta OSSRH_USERNAME; make -n release-build OK"]
```

Makefile incluye build-helpers/release.mk. Justfile sin lógica (delega en scripts).

### TASK-RELEASE-LOCAL-0005 - Endurecer deploy-to-github-packages.sh

```toml
id = "TASK-RELEASE-LOCAL-0005"
title = "Endurecer deploy-to-github-packages.sh"
state = "done"
priority = "p2"
owner = "rafex"
labels = []
dependencies = []
expected_files = ["scripts/deploy-to-github-packages.sh"]
close_criteria = "settings.xml con chmod 600 + trap; passphrase vía settings.xml (no CLI)."
validation = ["bash -n scripts/deploy-to-github-packages.sh"]
completion_evidence = ["bash -n OK; chmod 600 + trap cleanup_on_exit; gpg.passphrase vía settings.xml"]
```

### TASK-RELEASE-LOCAL-0006 - Validación y documentación

```toml
id = "TASK-RELEASE-LOCAL-0006"
title = "Validación y documentación"
state = "done"
priority = "p1"
owner = "rafex"
labels = []
dependencies = ["TASK-RELEASE-LOCAL-0004", "TASK-RELEASE-LOCAL-0005"]
expected_files = ["README.md", "spec-native/COMMANDS.md", "spec-native/decisions/DEC-0008-*.md"]
close_criteria = "Docs actualizadas, DEC-0008 registrada, dry-run y validación OK."
validation = ["make validate-main-build", "just publish-central --dry-run"]
completion_evidence = ["README/COMMANDS/DEC-0008 actualizados; make validate-main-build 29/29"]
```
