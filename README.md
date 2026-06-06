# ether-archetype

Maven archetype que genera proyectos Java con **arquitectura hexagonal** sobre el framework [Ether](https://rafex.dev).

Produce un proyecto multi-módulo listo para producción con:

- Estructura hexagonal (puertos y adaptadores) con reglas de dependencia verificadas en tiempo de compilación por **ArchUnit**
- Servidor HTTP **Jetty 12** integrado
- Persistencia **PostgreSQL** vía HikariCP
- Calidad de código: **Spotless**, **Checkstyle**, **OWASP Dependency Check**
- Ciclo de desarrollo con **Maven Wrapper**, **GNU Make** (compilación) y **Just** (tareas)

---

## Inicio rápido

### Prerrequisitos

| Herramienta | Versión mínima | Instalación |
|-------------|---------------|-------------|
| Java        | 21            | [sdkman.io](https://sdkman.io) |
| Maven       | 3.9           | [maven.apache.org](https://maven.apache.org) o `./mvnw` incluido |
| just        | 1.x           | `brew install just` / [just.systems](https://just.systems) |

### Generar un proyecto

```bash
mvn archetype:generate \
  -DarchetypeGroupId=dev.rafex.ether \
  -DarchetypeArtifactId=ether-hexagonal-archetype \
  -DarchetypeVersion=1.0.0 \
  -DgroupId=com.example \
  -DartifactId=myapp \
  -Dversion=0.1.0-SNAPSHOT \
  -Dpackage=com.example.myapp \
  -DjavaVersion=21 \
  -DetherVersion=9.0.0 \
  -DinteractiveMode=false
```

> **Importante**: `artifactId` es el **nombre base** del proyecto (ej. `myapp`).  
> El arquetipo genera automáticamente módulos como `myapp-core`, `myapp-ports`, `myapp-transport-jetty`, etc.

### Parámetros disponibles

| Parámetro      | Valor por defecto   | Descripción                              |
|----------------|---------------------|------------------------------------------|
| `groupId`      | `com.example`       | Group ID de Maven                        |
| `artifactId`   | `myapp-parent`      | Nombre base (sin sufijo `-parent`)       |
| `version`      | `0.1.0-SNAPSHOT`    | Versión inicial del proyecto             |
| `package`      | `com.example.myapp` | Paquete raíz de Java                     |
| `javaVersion`  | `21`                | Versión de Java                          |
| `etherVersion` | `9.0.0`             | Versión del framework Ether              |

---

## Estructura generada

```
myapp-parent/
├── .gitignore
├── .mvn/
│   └── wrapper/
│       ├── .gitignore
│       └── maven-wrapper.properties
├── mvnw                          ← Maven Wrapper (ejecutable)
├── mvnw.cmd                      ← Maven Wrapper (Windows)
├── Makefile                      ← Build system (compile, test, quality)
├── Justfile                      ← Task runner (run, docker, db)
├── pom.xml                       ← Parent POM con dependencyManagement
├── config/
│   ├── checkstyle/               ← Reglas Checkstyle
│   ├── owasp/                    ← Supresiones OWASP
│   └── spotless/                 ← Cabecera de licencia Spotless
├── myapp-ports/                  ← Interfaces del dominio (puertos)
├── myapp-common/                 ← Config, errores, utilidades compartidas
├── myapp-core/                   ← Lógica de negocio (servicios)
├── myapp-infra-postgres/         ← Implementaciones de repositorios (adaptador)
├── myapp-bootstrap/              ← Inyección de dependencias manual
├── myapp-transport-jetty/        ← Servidor HTTP Jetty (adaptador de entrada)
├── myapp-transport-grpc/         ← Placeholder gRPC
├── myapp-transport-rabbitmq/     ← Placeholder RabbitMQ
├── myapp-tools/                  ← Herramientas / scripts de soporte
└── myapp-architecture-tests/    ← Tests ArchUnit (reglas arquitectónicas)
```

---

## Comandos habituales

### Build (via Make)

```bash
make build          # Compilar todos los módulos
make test           # Ejecutar tests
make clean          # Limpiar artefactos
make format         # Aplicar formato Spotless
make format-check   # Verificar formato sin modificar
make quality        # Checks completos (spotless + checkstyle + owasp)
make install        # Instalar en repo local Maven (sin tests)
make install-full   # Instalar con tests
make package        # Empaquetar fat-jar de transport-jetty
```

### Ciclo de desarrollo (via Just)

```bash
just              # Ver todos los comandos disponibles
just run          # Compilar y ejecutar la aplicación
just run-debug    # Ejecutar con nivel DEBUG
just watch        # Reiniciar al detectar cambios en .java (requiere entr)
just docker-build # Construir imagen Docker
just up           # Levantar servicios con docker compose
just down         # Parar servicios
just migrate      # Aplicar migraciones de BD
just db-shell     # Shell interactivo PostgreSQL
just ci           # Pipeline CI completo
```

---

## Documentación detallada

| Documento | Contenido |
|-----------|-----------|
| [docs/architecture.md](docs/architecture.md) | Arquitectura hexagonal, capas, reglas de dependencia |
| [docs/modules.md](docs/modules.md) | Descripción detallada de cada módulo generado |
| [docs/usage.md](docs/usage.md) | Guía de uso completa con ejemplos |
| [docs/tooling.md](docs/tooling.md) | Make, Just, Maven Wrapper, calidad de código |
| [docs/extending.md](docs/extending.md) | Cómo agregar entidades, puertos y adaptadores |
| [docs/internals.md](docs/internals.md) | Funcionamiento interno del arquetipo Maven |

---

## Construir e instalar el arquetipo

```bash
cd ether-archetype
./mvnw clean install
```

Esto instala `dev.rafex.ether:ether-hexagonal-archetype:1.0.0` en tu repositorio Maven local,
quedando disponible para `mvn archetype:generate`.

---

## Licencia

Apache 2.0 — ver [LICENSE](https://www.apache.org/licenses/LICENSE-2.0.txt).
