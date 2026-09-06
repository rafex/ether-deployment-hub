+++
doctype = "decision"
id = "DEC-0005"
title = "Estrategia dual de GlowRoot"
status = "accepted"
created_at = "2026-09-06"
owners = []
related_specs = []
related_tasks = []
related_architecture = []
supersedes = []
tags = ["glowroot", "apm", "jdk25"]
+++

# DEC-0005 - Estrategia dual de GlowRoot

## Contexto

GlowRoot estable (0.14.7, con soporte JDK 25) solo se distribuye como ZIP desde GitHub Releases; la última API en Maven Central es 0.14.0-beta.3.

## Decisión

`glowroot-agent-api` queda en 0.14.0-beta.3 como dependencia compile-only; en runtime se usa `glowroot.jar >= 0.14.7` (SHA256 verificado) con `-XX:+EnableDynamicAgentLoading`.

## Consecuencias

Build compatible con Central; runtime requiere descargar el ZIP desde GitHub Releases y el flag JVM porque JDK 24+ bloquea dynamic agent loading por defecto.
