+++
doctype = "convention"
id = "CONV-0003"
title = "Commits y versionado convencional"
status = "active"
created_at = "2026-09-06"
related_architecture = []
related_decisions = []
tags = ["commits", "versioning", "conventional-commits"]
+++

# CONV-0003 - Commits y versionado convencional

## Justificación

El bump semántico automático del release planning depende de Conventional Commits.

## Regla

Mensajes tipo `feat:`, `fix:`, `perf:`, `docs:`, `chore:` con scope opcional (ej. `perf(tools-remote):`). Release policy: `docsOnly`/`testsOnly` no liberan versión; `buildOnly` = patch; `dependencyPropagation` = patch.

## Consecuencias

Release planning determinista; cada módulo libera según sus propios cambios detectados.
