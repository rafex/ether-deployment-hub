+++
doctype = "architecture"
id = "ARCH-0002"
title = "Motor de release planning"
status = "active"
created_at = "2026-09-06"
related_decisions = []
related_specs = []
tags = ["release", "planning", "scripts"]
+++

# ARCH-0002 - Motor de release planning

## Contexto

Detectar cambios por módulo y planear bumps de versión sin intervención manual.

## Diseño

Scripts `generate-release-plan.sh`, `apply-release-plan.sh`, `validate-release-plan-against-central.sh` + biblioteca común `release-common.sh` (`set -euo pipefail`, idempotencia, validación contra Maven Central). Make targets: `make release-plan`, `make deploy`, `make sync-manifest`, `make verify-central`.

## Restricciones y consecuencias

Plan dinámico y module-aware; validación de colisiones pre-deploy y pre-publish; `release-common.sh` centraliza utilidades reutilizables.
