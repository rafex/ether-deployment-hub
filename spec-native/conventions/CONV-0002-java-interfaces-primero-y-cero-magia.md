+++
doctype = "convention"
id = "CONV-0002"
title = "Java: interfaces primero y cero magia"
status = "active"
created_at = "2026-09-06"
related_architecture = []
related_decisions = []
tags = ["java", "principios", "naming"]
+++

# CONV-0002 - Java: interfaces primero y cero magia

## Justificación

Principios de diseño del ecosistema Ether (sin frameworks pesados).

## Regla

Cada capa expone contratos (interfaces), no implementaciones; sin reflexión en runtime ni anotaciones de framework; implementaciones en paquete `internal`; paquetes raíz `dev.rafex.ether.*`; sin statics ocultos para facilitar mockeo; aprovechar records, sealed classes y pattern matching de JDK 25.

## Consecuencias

Adopción incremental y testeable; consistencia de paquetes; bajo acoplamiento entre módulos.
