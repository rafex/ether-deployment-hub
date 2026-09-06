+++
doctype = "decision"
id = "DEC-0003"
title = "Chunking de niveles de despliegue"
status = "accepted"
created_at = "2026-09-06"
owners = []
related_specs = []
related_tasks = []
related_architecture = []
supersedes = []
tags = ["ci", "deploy", "topological-sort"]
+++

# DEC-0003 - Chunking de niveles de despliegue

## Contexto

GitHub Actions limita los jobs a 30 min; el release completo de 29 módulos excedía el límite.

## Decisión

`compute-deploy-levels.sh` aplica topological sort (Kahn) sobre el grafo de dependencias y chunking con `MAX_LEVEL_SIZE=5`, produciendo niveles L0..L7 desplegados secuencialmente.

## Consecuencias

Evita timeouts de 30 min; el orden de dependencias se respeta por niveles; máximo 5 módulos por job. Desglose: L0(1) L1(5) L2(5) L3(2) L4(5) L5(5) L6(3) L7(1).
