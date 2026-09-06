+++
doctype = "decision"
id = "DEC-0007"
title = "Versionado independiente y release policy por Conventional Commits"
status = "accepted"
created_at = "2026-09-06"
owners = []
related_specs = []
related_tasks = []
related_architecture = []
supersedes = []
tags = ["versioning", "conventional-commits", "release"]
+++

# DEC-0007 - Versionado independiente y release policy por Conventional Commits

## Contexto

Cada módulo evoluciona a ritmo propio; se necesita bump semántico automático.

## Decisión

`versioningStrategy: independent` en `releases/manifest.json`; release policy con `conventionalCommits: true`, `dependencyPropagation: patch`, `docsOnly: none`, `testsOnly: none`, `buildOnly: patch`.

## Consecuencias

Bump semántico por módulo según detección de cambios; las dependencias propagan patch; cambios de solo docs o tests no liberan versión.
