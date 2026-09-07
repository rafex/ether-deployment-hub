# TASKS.md

```toml
artifact_type = "task_file"
initiative = "ci-containers"
spec_id = "SPEC-0003"
owner = "rafex"
state = "in_progress"
```

## Tareas

### TASK-CI-CONTAINERS-0001 - Imagen CI/CD y helper container.sh

```toml
id = "TASK-CI-CONTAINERS-0001"
title = "Imagen CI/CD y helper container.sh"
state = "in_progress"
priority = "p1"
owner = "rafex"
labels = []
dependencies = []
expected_files = ["containers/ci/Containerfile", "scripts/container.sh"]
close_criteria = "Imagen con toolchain completo (JDK 25 + git/jq/python3/perl/make/gnupg/maven/just) y helper con acciones runtime|image|image-pull|ci|run."
validation = ["bash -n scripts/container.sh", "scripts/container.sh --action runtime"]
```

### TASK-CI-CONTAINERS-0002 - Fachadas container.mk + Just

```toml
id = "TASK-CI-CONTAINERS-0002"
title = "Fachadas container.mk + Just"
state = "in_progress"
priority = "p1"
owner = "rafex"
labels = []
dependencies = ["TASK-CI-CONTAINERS-0001"]
expected_files = ["build-helpers/container.mk", "build-helpers/just/release.just", "Makefile"]
close_criteria = "make runtime|image|image-pull|ci y just ci|release|release-host delegan en container.sh."
validation = ["make -n ci", "just --list"]
```

### TASK-CI-CONTAINERS-0003 - Workflow ghcr.io

```toml
id = "TASK-CI-CONTAINERS-0003"
title = "Workflow ghcr.io"
state = "in_progress"
priority = "p1"
owner = "rafex"
labels = []
dependencies = ["TASK-CI-CONTAINERS-0001"]
expected_files = [".github/workflows/build-ci-image.yml"]
close_criteria = "Workflow build & push a ghcr.io/rafex/ether-deployment-hub/ci (latest + SHA)."
validation = ["YAML válido; permisos packages: write"]
```

### TASK-CI-CONTAINERS-0004 - Validación y documentación

```toml
id = "TASK-CI-CONTAINERS-0004"
title = "Validación y documentación"
state = "todo"
priority = "p1"
owner = "rafex"
labels = []
dependencies = ["TASK-CI-CONTAINERS-0002", "TASK-CI-CONTAINERS-0003"]
expected_files = ["README.md", "spec-native/COMMANDS.md", "spec-native/decisions/DEC-0009-*.md", "spec-native/ROADMAP.md", "spec-native/intake/IDEAS.md"]
close_criteria = "Docs actualizadas, DEC-0009 registrada, deuda workflows→wrappers documentada, make ci 29/29 en contenedor."
validation = ["make image", "make ci", "just release --dry-run"]
```
