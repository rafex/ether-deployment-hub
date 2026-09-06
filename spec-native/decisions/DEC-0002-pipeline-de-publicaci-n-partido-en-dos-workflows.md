+++
doctype = "decision"
id = "DEC-0002"
title = "Pipeline de publicación partido en dos workflows"
status = "accepted"
created_at = "2026-09-06"
owners = []
related_specs = []
related_tasks = []
related_architecture = []
supersedes = []
tags = ["ci", "publish", "maven-central", "github-packages"]
+++

# DEC-0002 - Pipeline de publicación partido en dos workflows

## Contexto

Publicar a Maven Central y espejar a GitHub Packages sin duplicar builds ni re-publicar.

## Decisión

Dos workflows independientes: `publish-java-modules-maven-central.yml` (por tag o workflow_dispatch) publica a Central y sube `release-plan` + artefactos `maven-artifacts-level-N`; `publish-java-modules-gh-pkg.yml` se dispara por `workflow_run` (success) y reusa esos artefactos restaurando `~/.m2` y firmando con GPG hacia `maven.pkg.github.com`.

## Consecuencias

Un único build/publish a Central; mirror a GH Packages sin polling a Central; re-run manual reproducible vía `workflow_dispatch` con `maven_central_run_id`.
