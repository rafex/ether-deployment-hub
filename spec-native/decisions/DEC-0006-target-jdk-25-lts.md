+++
doctype = "decision"
id = "DEC-0006"
title = "Target JDK 25 LTS"
status = "accepted"
created_at = "2026-09-06"
owners = []
related_specs = []
related_tasks = []
related_architecture = []
supersedes = []
tags = ["java", "jdk25"]
+++

# DEC-0006 - Target JDK 25 LTS

## Contexto

El target era Java 21; se migró a 25 LTS (Temurin) en la rama `migrate-jdk25`.

## Decisión

Compilar y correr sobre JDK 25 (Temurin); CI con `actions/setup-java@v5` (`distribution: temurin`, `java-version: '25'`); Maven Wrapper 3.9.12.

## Consecuencias

Se habilitan mejoras JEP 441/444/456 (ya aplicadas en 5 módulos); preview features (JEP 505, 506, 488) pendientes de estabilización.
