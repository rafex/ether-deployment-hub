+++
doctype = "decision"
id = "DEC-0001"
title = "Migración de Git Submodules a Git Subtrees"
status = "accepted"
created_at = "2026-09-06"
owners = []
related_specs = []
related_tasks = []
related_architecture = []
supersedes = []
tags = ["git", "subtrees", "trazabilidad"]
+++

# DEC-0001 - Migración de Git Submodules a Git Subtrees

## Contexto

El hub alojaba 27 módulos como submodules; el checkout recursivo rompía CI y complicaba el pipeline de release (requería `git submodule update`).

## Decisión

Importar cada módulo con `git subtree add --squash` al mismo path que ocupaba el submodule, registrar repo/rama/SHA de origen en `releases/subtrees.json` y adoptar flujo pull-only (`git subtree pull --squash`) desde los repos originales. La copia histórica de `.gitmodules` quedó en `docs/archive/`.

## Consecuencias

Historial fino fuera del hub (se consulta en el repo original con el SHA registrado); checkout simple sin `submodule update`; trazabilidad vía `subtrees.json` validada por `validate-source-refs` contra los remotos.
