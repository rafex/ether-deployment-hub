# TASKS.md

```toml
artifact_type = "task_file"
initiative = "release-local-first"
spec_id = "SPEC-0002"
owner = "rafex"
state = "in_progress"
```

## Tareas

### TASK-RELEASE-LOCAL-0001 - Fix orden de install-all-modules.sh

```toml
id = "TASK-RELEASE-LOCAL-0001"
title = "Fix orden de install-all-modules.sh"
state = "in_progress"
priority = "p1"
owner = "rafex"
labels = []
dependencies = []
expected_files = ["scripts/install-all-modules.sh"]
close_criteria = "El orden de instalación se deriva del topo-sort canónico (29 módulos) y make validate-main-build pasa."
validation = ["make validate-main-build"]
```

Eliminar el array hardcodeado de 27 módulos; derivar el orden de
`compute-deploy-levels.sh`/manifest. Añadir `ether-cron` y
`ether-websocket-proxy-jetty12`. `set -euo pipefail`, mktemp+trap.

### TASK-RELEASE-LOCAL-0002 - Secretos sops+age

```toml
id = "TASK-RELEASE-LOCAL-0002"
title = "Secretos sops+age"
state = "in_progress"
priority = "p1"
owner = "rafex"
labels = []
dependencies = []
expected_files = [".sops.yaml", ".secrets/secrets.maven.enc.yaml", "scripts/secrets.sh"]
close_criteria = "just/env descifran OSSRH_* y GPG sin leak; clave age en ~/.age/ether-deployment-hub-key.txt."
validation = ["scripts/secrets.sh --action verify"]
```

Clave age en `~/.age/ether-deployment-hub-key.txt`; `.sops.yaml` con recipients;
`.secrets/secrets.maven.enc.yaml` con OSSRH_USERNAME, OSSRH_PASSWORD,
MAVEN_GPG_PASSPHRASE, GPG_PRIVATE_KEY_B64.

### TASK-RELEASE-LOCAL-0003 - Orquestador local deploy-release.sh

```toml
id = "TASK-RELEASE-LOCAL-0003"
title = "Orquestador local deploy-release.sh"
state = "in_progress"
priority = "p1"
owner = "rafex"
labels = []
dependencies = ["TASK-RELEASE-LOCAL-0001"]
expected_files = ["scripts/deploy-release.sh"]
close_criteria = "Ejecuta plan→validate→apply→niveles→make deploy→update-manifest con --dry-run funcional."
validation = ["scripts/deploy-release.sh --dry-run"]
```

Porta la lógica de deploy-one-level.yml. Flags --base-ref/--head-ref/--dry-run/
--levels/--skip-tests/--log-file. `CENTRAL_WAIT_UNTIL=validated`.

### TASK-RELEASE-LOCAL-0004 - Fachadas release.mk + Justfile

```toml
id = "TASK-RELEASE-LOCAL-0004"
title = "Fachadas release.mk + Justfile"
state = "in_progress"
priority = "p1"
owner = "rafex"
labels = []
dependencies = ["TASK-RELEASE-LOCAL-0002", "TASK-RELEASE-LOCAL-0003"]
expected_files = ["build-helpers/release.mk", "Justfile", "build-helpers/just/release.just"]
close_criteria = "make release-build y just env maven/publish-central/verify-published/release existen y delegan."
validation = ["make release-build --dry-run", "just --list"]
```

Makefile incluye build-helpers/release.mk. Justfile sin lógica (delega en scripts).

### TASK-RELEASE-LOCAL-0005 - Endurecer deploy-to-github-packages.sh

```toml
id = "TASK-RELEASE-LOCAL-0005"
title = "Endurecer deploy-to-github-packages.sh"
state = "in_progress"
priority = "p2"
owner = "rafex"
labels = []
dependencies = []
expected_files = ["scripts/deploy-to-github-packages.sh"]
close_criteria = "settings.xml con chmod 600 + trap; passphrase vía settings.xml (no CLI)."
validation = ["bash -n scripts/deploy-to-github-packages.sh"]
```

### TASK-RELEASE-LOCAL-0006 - Validación y documentación

```toml
id = "TASK-RELEASE-LOCAL-0006"
title = "Validación y documentación"
state = "todo"
priority = "p1"
owner = "rafex"
labels = []
dependencies = ["TASK-RELEASE-LOCAL-0004", "TASK-RELEASE-LOCAL-0005"]
expected_files = ["README.md", "spec-native/COMMANDS.md", "spec-native/decisions/DEC-0008-*.md"]
close_criteria = "Docs actualizadas, DEC-0008 registrada, dry-run y validación OK."
validation = ["make validate-main-build", "just publish-central --dry-run"]
```
