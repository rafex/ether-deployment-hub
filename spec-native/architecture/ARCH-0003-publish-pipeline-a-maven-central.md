+++
doctype = "architecture"
id = "ARCH-0003"
title = "Publish pipeline a Maven Central"
status = "active"
created_at = "2026-09-06"
related_decisions = []
related_specs = []
tags = ["ci", "maven-central", "workflow"]
+++

# ARCH-0003 - Publish pipeline a Maven Central

## Contexto

Publicar artefactos firmados a Maven Central en orden de dependencias.

## Diseño

Workflow `publish-java-modules-maven-central.yml`: jobs `release-plan` → `deploy-level-0..6` (cada uno llama `deploy-one-level.yml`, recolecta JARs/POMs de `~/.m2` y sube `maven-artifacts-level-N`) → `update-manifest`. Permissions `contents: write`. `make publish-ci` dispara el workflow.

## Restricciones y consecuencias

Deploy por niveles; artefactos reutilizables downstream; manifest actualizado al final; dry-run disponible con `make publish-plan-ci`.
