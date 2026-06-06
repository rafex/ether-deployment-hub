# Arquitectura Hexagonal

## ¿Qué es la arquitectura hexagonal?

La **arquitectura hexagonal** (también llamada *Ports and Adapters*, propuesta por Alistair Cockburn) separa el núcleo de la aplicación —la lógica de negocio— de todo lo que interactúa con el mundo exterior: bases de datos, HTTP, mensajería, CLI, etc.

El objetivo es que el dominio nunca dependa de detalles de infraestructura. Los detalles dependen del dominio, nunca al revés.

```
         ┌─────────────────────────────────────────────────┐
         │                   EXTERIOR                       │
         │  HTTP · gRPC · RabbitMQ · CLI · Tests           │
         └───────────────────┬─────────────────────────────┘
                             │  usa
                    ┌────────▼────────┐
                    │   Transport /   │  Adaptadores de ENTRADA
                    │   Bootstrap     │  (llaman al Core)
                    └────────┬────────┘
                             │  usa
                    ┌────────▼────────┐
                    │      CORE       │  Servicios de dominio
                    │   (servicios)   │  Sólo conoce Ports
                    └────────┬────────┘
                             │  define
                    ┌────────▼────────┐
                    │     PORTS       │  Interfaces (contratos)
                    └────────┬────────┘
                             │  implementa
                    ┌────────▼────────┐
                    │  Infra Adapters │  Adaptadores de SALIDA
                    │  (PostgreSQL…)  │  (implementan Ports)
                    └─────────────────┘
```

### Common (transversal)

`common` contiene configuración, errores compartidos y utilidades. No depende de ninguna otra capa del proyecto.

---

## Capas y módulos

| Capa       | Módulo Maven              | Rol                                          |
|------------|---------------------------|----------------------------------------------|
| Ports      | `{app}-ports`             | Interfaces de dominio (repositorios, etc.)   |
| Common     | `{app}-common`            | Config, errores, DTOs compartidos            |
| Core       | `{app}-core`              | Lógica de negocio, usa sólo Ports            |
| Infra      | `{app}-infra-postgres`    | Implementa Ports con PostgreSQL/HikariCP     |
| Bootstrap  | `{app}-bootstrap`         | Inyección de dependencias manual (DI)        |
| Transport  | `{app}-transport-jetty`   | Adaptador de entrada HTTP (Jetty 12)         |
| Transport  | `{app}-transport-grpc`    | Placeholder adaptador gRPC                  |
| Transport  | `{app}-transport-rabbitmq`| Placeholder adaptador RabbitMQ              |
| Tools      | `{app}-tools`             | Scripts, seeds, herramientas auxiliares      |
| Arch Tests | `{app}-architecture-tests`| Tests ArchUnit — validan las reglas          |

---

## Reglas de dependencia

Las reglas se verifican automáticamente en cada build mediante **ArchUnit**. Ningún PR puede romperlas sin que los tests fallen.

### Diagrama de dependencias permitidas

```
  [common]  ──────────────────────────────────────────┐
                                                       │ (sin dependencias outbound)
  [ports]   ──────────────────────────────────────────┤
                                                       │ (interfaces puras)
  [core]    ─── depende de ──► [ports]                │
                                                       │
  [infra]   ─── depende de ──► [ports]                │
                                                       │
  [bootstrap] ─ depende de ──► [core] + [infra]       │
                                                       │
  [transport] ─ depende de ──► [core] + [ports]       │
                ─ depende de ──► [common]              │
```

### Reglas ArchUnit implementadas

| Regla | Descripción |
|-------|-------------|
| `ports_must_not_depend_on_core_or_adapters` | Los puertos (interfaces) no pueden importar servicios ni implementaciones |
| `common_must_not_depend_on_core_or_adapters` | Config/errores no dependen de nada del dominio |
| `core_must_not_depend_on_adapters` | Los servicios no importan clases de infra, bootstrap ni transport |
| `infra_must_not_depend_on_transport_or_core_services` | La capa de datos no sabe nada de HTTP ni servicios |
| `transport_must_not_depend_on_infra_details` | Los handlers HTTP no importan clases de BD directamente |
| `bootstrap_must_not_depend_on_transport` | El DI container no depende de handlers ni del servidor |

---

## ¿Por qué esta arquitectura?

1. **Testabilidad**: el Core se puede probar sin BD ni servidor; basta con un mock del Port.
2. **Intercambiabilidad**: cambiar PostgreSQL por MongoDB sólo afecta a `infra-postgres`, sin tocar Core.
3. **Claridad de fronteras**: cualquier desarrollador puede ver de un vistazo qué capa puede importar qué.
4. **Seguridad ante el cambio**: ArchUnit hace que las violaciones de arquitectura sean errores de compilación.

---

## Flujo de una petición HTTP típica

```
HTTP Request
     │
     ▼
HealthHandler / MiHandler   (transport-jetty)
     │  llama a
     ▼
ExampleService              (core)
     │  llama a (Port interface)
     ▼
ExampleRepository           (ports — interface)
     │  implementado por
     ▼
ExampleRepositoryImpl       (infra-postgres)
     │  usa
     ▼
DatabaseClient / HikariCP   (infra-postgres — Db)
     │
     ▼
PostgreSQL
```

La inversión de dependencias ocurre entre `core` y `infra`: Core define la interfaz `ExampleRepository` (en `ports`) e Infra la implementa. Core nunca importa la implementación concreta.

---

## Gestión de errores

Toda la capa de dominio lanza `AppError` (checked) o `AppRuntimeError` (unchecked), ambos definidos en `common`. Los handlers de transporte los capturan y los convierten a respuestas HTTP con `ether-http-problem`.
