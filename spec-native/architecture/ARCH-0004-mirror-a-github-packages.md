+++
doctype = "architecture"
id = "ARCH-0004"
title = "Mirror a GitHub Packages"
status = "active"
created_at = "2026-09-06"
related_decisions = []
related_specs = []
tags = ["ci", "github-packages", "workflow_run"]
+++

# ARCH-0004 - Mirror a GitHub Packages

## Contexto

Espejar artefactos de Maven Central en GitHub Packages sin re-publicar.

## Diseño

Workflow `publish-java-modules-gh-pkg.yml`: job `setup` descarga `release-plan` del run de Central (por run-id), lo re-sube y recomputa niveles con `compute-deploy-levels.sh`; `deploy-gh-packages-level-0..6` restauran `maven-artifacts-level-N` a `~/.m2`, firman con GPG y despliegan a `maven.pkg.github.com`. `make publish-gh-pkg-ci RUN_ID=<id>` re-dispara.

## Restricciones y consecuencias

Sin polling a Maven Central; firma GPG con semáforo `MAX_GH_PARALLEL=4`; re-run reproducible desde un run id de Central.
