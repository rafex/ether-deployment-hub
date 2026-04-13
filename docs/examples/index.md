# Ejemplos de uso real

Esta sección muestra cómo los módulos de **Ether** se integran en proyectos de producción.
Cada ejemplo está basado en código real, no en fragmentos inventados.

En particular, los ejemplos priorizan patrones prácticos de:

- arranque de servidores HTTP,
- construcción de endpoints,
- seguridad,
- logging,
- trazabilidad y observabilidad.

## Guías por módulo

| Guía | Descripción |
|---|---|
| [ether-di](ether-di.md) | Lazy, Closer y Bootstrap — DI explícita sin reflexión ni anotaciones |

## Proyectos de ejemplo

| Proyecto | Descripción | Módulos usados |
|---|---|---|
| [Kiwi](kiwi.md) | Casos prácticos de servidor Jetty, endpoints, JWT, logging, health checks y APM | ether-config, ether-di, ether-jwt, ether-jdbc, ether-http-jetty12, ether-http-security, ether-glowroot-jetty12 |

---

## Patrón general de una aplicación Ether

Toda aplicación Ether sigue la misma estructura de capas:

```
                  ┌──────────────────────────────┐
                  │        Transport layer        │  ← ether-http-jetty12
                  │   (HTTP, CLI, gRPC, etc.)     │
                  └──────────────┬───────────────┘
                                 │
                  ┌──────────────▼───────────────┐
                  │        Bootstrap / DI         │  ← ether-di
                  │   (KiwiContainer, wiring)     │
                  └──────────────┬───────────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              │                  │                  │
  ┌───────────▼──────┐ ┌────────▼────────┐ ┌───────▼────────┐
  │   Core (domain)  │ │  Ports (APIs)   │ │  Infra (impl)  │
  │  use-cases, etc. │ │  interfaces     │ │  PostgreSQL, …  │
  └──────────────────┘ └─────────────────┘ └────────────────┘
```

El contenedor de dependencias (`Bootstrap`) crea y conecta todos los componentes.
El servidor HTTP recibe el contenedor ya inicializado y registra rutas, auth y middlewares.
