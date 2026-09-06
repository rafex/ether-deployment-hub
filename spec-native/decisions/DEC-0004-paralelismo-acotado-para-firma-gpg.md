+++
doctype = "decision"
id = "DEC-0004"
title = "Paralelismo acotado para firma GPG"
status = "accepted"
created_at = "2026-09-06"
owners = []
related_specs = []
related_tasks = []
related_architecture = []
supersedes = []
tags = ["gpg", "signing", "ci"]
+++

# DEC-0004 - Paralelismo acotado para firma GPG

## Contexto

En runners compartidos, firmar artefactos en paralelo sin límite agotaba memoria (`gpg: signing failed: Cannot allocate memory`).

## Decisión

`deploy-to-github-packages.sh` usa un semáforo con `MAX_GH_PARALLEL=4` de paralelismo acotado.

## Consecuencias

Firma estable sin fallos de memoria; throughput acotado a 4 firmas concurrentes.
