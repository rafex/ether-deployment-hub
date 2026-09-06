# TASKS.md

```toml
artifact_type = "task_file"
initiative = "contexto-docs"
spec_id = "SPEC-0001"
owner = "rafex"
state = "done"
```

## Tareas

### TASK-CONTEXTO-DOCS-0001 - Registrar 7 decisiones canónicas

```toml
id = "TASK-CONTEXTO-DOCS-0001"
title = "Registrar 7 decisiones canónicas"
state = "done"
priority = "p1"
owner = "rafex"
labels = []
dependencies = []
expected_files = ["spec-native/decisions/DEC-*.md"]
close_criteria = "Existen 7 archivos DEC-0001..DEC-0007 con front matter válido y DECISIONS.md con tabla regenerada."
validation = ["list_decisions() lista 7 decisiones con status accepted"]
completion_evidence = ["list_decisions() lista 7 decisiones DEC-0001..DEC-0007 con status accepted"]
```

Registrar vía `log_decision` las 7 decisiones persistentes: subtrees --squash,
publish split, level chunking, GPG paralelismo, GlowRoot dual, JDK 25 y versioning.

### TASK-CONTEXTO-DOCS-0002 - Crear 5 artefactos de arquitectura

```toml
id = "TASK-CONTEXTO-DOCS-0002"
title = "Crear 5 artefactos de arquitectura"
state = "done"
priority = "p1"
owner = "rafex"
labels = []
dependencies = ["TASK-CONTEXTO-DOCS-0001"]
expected_files = ["spec-native/architecture/ARCH-*.md"]
close_criteria = "Existen 5 archivos ARCH-0001..ARCH-0005 y ARCHITECTURE.md con tabla regenerada."
validation = ["list_architecture() lista 5 artefactos"]
completion_evidence = ["list_architecture() lista 5 artefactos ARCH-0001..ARCH-0005"]
```

Topología de módulos, release planning, publish Central, publish GH Packages y
análisis estático del codebase.

### TASK-CONTEXTO-DOCS-0003 - Crear 3 convenciones

```toml
id = "TASK-CONTEXTO-DOCS-0003"
title = "Crear 3 convenciones"
state = "done"
priority = "p2"
owner = "rafex"
labels = []
dependencies = ["TASK-CONTEXTO-DOCS-0002"]
expected_files = ["spec-native/conventions/CONV-*.md"]
close_criteria = "Existen 3 archivos CONV-0001..CONV-0003 y CONVENTIONS.md con tabla regenerada."
validation = ["list_conventions() lista 3 convenciones"]
completion_evidence = ["list_conventions() lista 3 convenciones CONV-0001..CONV-0003"]
```

Bash scripts robustos, Java interfaces-primero y commits/versionado convencional.

### TASK-CONTEXTO-DOCS-0004 - Actualizar STACK.md y ROADMAP.md

```toml
id = "TASK-CONTEXTO-DOCS-0004"
title = "Actualizar STACK.md y ROADMAP.md"
state = "done"
priority = "p2"
owner = "rafex"
labels = []
dependencies = ["TASK-CONTEXTO-DOCS-0003"]
expected_files = ["spec-native/STACK.md", "spec-native/ROADMAP.md"]
close_criteria = "STACK.md tiene versiones reales (Jackson 2.21.2, Jetty 12.1.7) y ROADMAP.md integra el plan de auditoría."
validation = ["health-check reporta has_real_content en stack y roadmap"]
completion_evidence = ["STACK.md con Jackson 2.21.2 y Jetty 12.1.7; ROADMAP.md con plan de auditoría"]
```

### TASK-CONTEXTO-DOCS-0005 - Trazabilidad, validación y entrega

```toml
id = "TASK-CONTEXTO-DOCS-0005"
title = "Trazabilidad, validación y entrega"
state = "done"
priority = "p1"
owner = "rafex"
labels = []
dependencies = ["TASK-CONTEXTO-DOCS-0004"]
expected_files = ["spec-native/TRACEABILITY.md"]
close_criteria = "TRACEABILITY.md actualizado, health-check healthy y cambios commiteados y pusheados a main."
validation = ["health-check healthy sin issues", "git push a origin/main"]
completion_evidence = ["TRACEABILITY.md actualizado; health-check healthy; commit + push a origin/main"]
```
