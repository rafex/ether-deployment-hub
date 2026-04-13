# Guía práctica: ether-hexagonal-archetype

**ether-hexagonal-archetype** es un arquetipo Maven que genera un proyecto multi-módulo
con arquitectura hexagonal (puertos y adaptadores) completo y listo para producción,
usando el framework Ether con Jetty 12 y PostgreSQL.

## Instalación

No se añade como dependencia — se usa para generar un nuevo proyecto:

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

---

## Parámetros

| Parámetro | Defecto | Descripción |
|---|---|---|
| `groupId` | `com.example` | Group ID de Maven |
| `artifactId` | `myapp-parent` | Nombre base del proyecto |
| `version` | `0.1.0-SNAPSHOT` | Versión inicial |
| `package` | `com.example.myapp` | Paquete raíz Java |
| `javaVersion` | `21` | Versión Java |
| `etherVersion` | `9.0.0` | Versión del framework Ether |

---

## Estructura generada

```
myapp-parent/
├── pom.xml                        ← Parent POM con dependencyManagement
├── Makefile                       ← Build: compile, test, quality, install
├── Justfile                       ← Tasks: run, docker, db, migrate
├── mvnw                           ← Maven Wrapper
├── config/
│   ├── checkstyle/                ← Reglas Checkstyle
│   ├── spotless/                  ← Cabecera de licencia
│   └── owasp/                     ← Supresiones OWASP
├── myapp-ports/                   ← Interfaces del dominio (puertos)
├── myapp-common/                  ← Config, errores, utilidades compartidas
├── myapp-core/                    ← Lógica de negocio (servicios, use-cases)
├── myapp-infra-postgres/          ← Repositorios JDBC (adaptador de salida)
├── myapp-bootstrap/               ← DI con ether-di (contenedor)
├── myapp-transport-jetty/         ← Servidor HTTP Jetty (adaptador de entrada)
├── myapp-transport-grpc/          ← Placeholder gRPC
├── myapp-transport-rabbitmq/      ← Placeholder RabbitMQ
├── myapp-tools/                   ← Scripts y herramientas de soporte
└── myapp-architecture-tests/      ← Tests ArchUnit (reglas arquitectónicas)
```

---

## Reglas de arquitectura (ArchUnit)

El módulo `myapp-architecture-tests` verifica en tiempo de compilación que:

- `myapp-core` no depende de `myapp-infra-*` ni de `myapp-transport-*`
- `myapp-ports` no depende de ningún módulo interno
- `myapp-transport-*` solo depende de `myapp-ports` y `myapp-bootstrap`

---

## Comandos habituales

### Build (Make)

```bash
make build          # Compilar todos los módulos
make test           # Ejecutar tests
make quality        # Checkstyle + Spotless + OWASP
make install        # Instalar en repositorio Maven local (sin tests)
make install-full   # Instalar con tests
make package        # Crear fat-jar del transport-jetty
```

### Desarrollo (Just)

```bash
just run            # Compilar y ejecutar la aplicación
just run-debug      # Ejecutar con LOG_LEVEL=DEBUG
just watch          # Reiniciar al detectar cambios .java (requiere entr)
just docker-build   # Construir imagen Docker
just up             # Levantar servicios con docker compose
just down           # Parar servicios
just migrate        # Aplicar migraciones de BD (Flyway/Liquibase)
just db-shell       # Shell interactivo PostgreSQL
just ci             # Pipeline CI completo
```

---

## Lo que incluye el proyecto generado

- **Arquitectura hexagonal** verificada por ArchUnit
- **Servidor HTTP Jetty 12** con ether-http-jetty12
- **PostgreSQL** con HikariCP y ether-jdbc
- **DI explícita** con ether-di (sin reflexión)
- **Config 12-factor** con ether-config
- **JWT** con ether-jwt
- **Logging** con ether-logging-core
- **Calidad de código**: Checkstyle, Spotless, OWASP Dependency Check
- **Docker** y `docker-compose.yml` preconfigurados

---

## Construir e instalar el arquetipo localmente

```bash
cd ether-archetype
./mvnw clean install
```

---

## Más información

- [Javadoc API](../api/doxygen/html/index.html)
- [Código fuente](https://github.com/rafex/ether-archetype)
