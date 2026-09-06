# PRODUCT.md

Fuente de verdad del producto.

## Problema

Publicar y mantener un ecosistema de ~29 librerías Java modulares en Maven
Central (y GitHub Packages) de forma reproducible, sin pasos manuales y
respetando el orden estricto de dependencias entre módulos.

## Usuarios

- **Equipo Ether** — publica nuevas versiones de los módulos tras cada cambio;
  necesita un pipeline de release que detecte qué cambió y despliegue en orden.
- **Consumidores de las librerías** — dependen de artefactos estables y
  versionados en Maven Central (`dev.rafex.ether.*`).

## Objetivos

- Release planning dinámico y module-aware (detección de cambios + bump semántico).
- Deploy idempotente en orden de dependencias, con validación de colisiones contra Maven Central antes de deploy y re-validación antes de publish.
- Pipeline de publicación partido en dos workflows independientes: Maven Central y GitHub Packages.

## No objetivos

- No es un framework de aplicación: los módulos son librerías independientes, sin Spring/Quarkus/Micronaut.
- No aloja el desarrollo de cada módulo (viven en subtrees con repos propios); este repo solo orquesta build y publicación.

## Valor diferencial

Cero magia y adopción incremental. El hub elimina el toil de publicar 29
artefactos interdependientes en el orden correcto, con idempotencia y
verificación previa de colisiones en Maven Central.
