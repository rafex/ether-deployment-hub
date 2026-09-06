+++
doctype = "convention"
id = "CONV-0001"
title = "Scripts Bash robustos e idempotentes"
status = "active"
created_at = "2026-09-06"
related_architecture = []
related_decisions = []
tags = ["bash", "scripts", "pipeline"]
+++

# CONV-0001 - Scripts Bash robustos e idempotentes

## Justificación

El pipeline depende de 16 scripts Bash; la auditoría (`docs/automation-audit-summary.md`) destacó fortalezas (fail-fast, portabilidad) y riesgos (testing 2/10).

## Regla

`set -euo pipefail` al inicio; `mktemp` + `trap` para limpieza; idempotencia (skip de operaciones ya hechas); portabilidad Linux/macOS/Docker; validación contra estado externo (Maven Central); funciones comunes centralizadas en `release-common.sh`.

## Consecuencias

Fail-fast y sin efectos secundarios; deuda pendiente: logging estructurado, tests unitarios de scripts y retries/backoff en operaciones de red.
