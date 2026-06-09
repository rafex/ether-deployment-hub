# Guía de uso

## Prerrequisitos

| Herramienta | Versión mínima | Notas                                  |
|-------------|---------------|----------------------------------------|
| Java JDK    | 21            | Recomendado via [sdkman.io](https://sdkman.io) |
| Maven       | 3.9.x         | O usar `./mvnw` incluido en el proyecto |
| just        | 1.x           | `brew install just` (macOS/Linux) / [just.systems](https://just.systems) |
| Docker      | 24+           | Sólo para `just docker-build`, `just up` |
| entr        | opcional      | Para `just watch`: `brew install entr` |

---

## Instalar el arquetipo

### Desde repositorio local

```bash
# clonar (o entrar al directorio) del módulo ether-archetype
cd ether-deployment-hub/ether-archetype

# instalar en el repositorio Maven local (~/.m2)
./mvnw clean install

# verificar la instalación
mvn archetype:generate -Dfilter=dev.rafex.ether:
```

### Desde repositorio remoto (cuando esté publicado)

```bash
mvn archetype:generate \
  -DarchetypeGroupId=dev.rafex.ether \
  -DarchetypeArtifactId=ether-hexagonal-archetype \
  -DarchetypeVersion=1.0.0
```

---

## Generar un nuevo proyecto

### Modo no interactivo (recomendado para CI / scripts)

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

### Modo interactivo

```bash
mvn archetype:generate \
  -DarchetypeGroupId=dev.rafex.ether \
  -DarchetypeArtifactId=ether-hexagonal-archetype \
  -DarchetypeVersion=1.0.0
```

Maven preguntará los valores de cada propiedad. Pulsa `Enter` para aceptar el valor por defecto.

### Convención de nombres

| Parámetro     | Correcto          | Incorrecto           | Motivo                                      |
|---------------|-------------------|----------------------|---------------------------------------------|
| `artifactId`  | `myapp`           | `myapp-parent`       | El arquetipo agrega `-parent` al POM raíz   |
| `artifactId`  | `order-service`   | `OrderService`       | Maven usa kebab-case                        |
| `package`     | `com.example.myapp` | `com.example`      | Debe incluir el nombre de la app            |

---

## Primera compilación

```bash
cd myapp-parent

# compilar todo el proyecto
make build
# equivalente a: ./mvnw compile
```

### Ejecutar los tests (incluyendo ArchUnit)

```bash
make test
# equivalente a: ./mvnw test
```

### Instalar en el repo local Maven

```bash
make install
# equivalente a: ./mvnw clean install -DskipTests
```

---

## Ejecutar la aplicación

### Opción 1: via Just (recomendado)

```bash
# configura las variables de entorno
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=appdb
export DB_USER=postgres
export DB_PASSWORD=secret
export SERVER_PORT=8080

just run
```

Just compilará el fat-jar de `transport-jetty` y ejecutará `java -jar`.

### Opción 2: directamente

```bash
make package
java -jar myapp-transport-jetty/target/myapp-transport-jetty-0.1.0-SNAPSHOT-jar-with-dependencies.jar
```

### Opción 3: con Docker

```bash
# primero crea un Dockerfile en la raíz del proyecto generado
just docker-build
just docker-run
```

### Opciones de log

```bash
just run              # nivel INFO (defecto)
just run-debug        # nivel DEBUG
just run-log WARN     # nivel WARN
```

---

## Verificar que funciona

Una vez iniciada la aplicación:

```bash
curl http://localhost:8080/health
# {"status":"UP"}
```

---

## Configuración de la aplicación

La aplicación se configura íntegramente mediante **variables de entorno** (sin archivos de propiedades externos en runtime). Esto la hace compatible con contenedores y plataformas cloud.

### Variables disponibles

| Variable              | Defecto       | Descripción                          |
|-----------------------|---------------|--------------------------------------|
| `DB_HOST`             | `localhost`   | Host de PostgreSQL                   |
| `DB_PORT`             | `5432`        | Puerto de PostgreSQL                 |
| `DB_NAME`             | `appdb`       | Nombre de la base de datos           |
| `DB_USER`             | `postgres`    | Usuario de BD                        |
| `DB_PASSWORD`         | *(vacío)*     | Contraseña de BD                     |
| `DB_POOL_SIZE`        | `10`          | Tamaño máximo del pool HikariCP      |
| `DB_CONNECTION_TIMEOUT` | `30000`     | Timeout de conexión (ms)             |
| `SERVER_HOST`         | `0.0.0.0`     | Interfaz de escucha del servidor     |
| `SERVER_PORT`         | `8080`        | Puerto del servidor HTTP             |

### Archivo `.env` para desarrollo local

Crea un `.env` en la raíz del proyecto (ya en `.gitignore`):

```dotenv
DB_HOST=localhost
DB_PORT=5432
DB_NAME=myapp_dev
DB_USER=myapp
DB_PASSWORD=secret
SERVER_PORT=8080
```

Para cargarlo automáticamente, usa `direnv` o exporta manualmente:

```bash
export $(grep -v '^#' .env | xargs)
```

---

## Ciclo de desarrollo con docker compose

Crea un `docker-compose.yml` en la raíz con PostgreSQL:

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: myapp_dev
      POSTGRES_USER: myapp
      POSTGRES_PASSWORD: secret
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

Luego:

```bash
just up        # levanta PostgreSQL
just migrate   # aplica migraciones (configura Flyway/Liquibase en tools)
just run       # inicia la aplicación
just down      # para todo
```

---

## Calidad de código

### Formato automático

```bash
make format         # aplica Spotless
make format-check   # verifica sin modificar (falla en CI si hay diferencias)
```

### Checks completos (CI)

```bash
make quality
# equivalente a: ./mvnw -Pquality verify
# Ejecuta: spotless:check + checkstyle + owasp dependency-check
```

> **OWASP Dependency Check** requiere una API key de NVD:
> ```bash
> export NVD_API_KEY=tu-api-key
> make quality
> ```
> Obtenla en: https://nvd.nist.gov/developers/request-an-api-key

### Pipeline CI completo

```bash
just ci
# equivalente a: make quality (que incluye tests)
```

---

## Escenarios de uso múltiple

### Varios servicios desde el mismo arquetipo

```bash
# servicio de usuarios
mvn archetype:generate ... -DartifactId=users-service -Dpackage=com.example.users

# servicio de pedidos
mvn archetype:generate ... -DartifactId=orders-service -Dpackage=com.example.orders
```

Cada uno es un proyecto Maven multi-módulo independiente.

### Diferentes versiones de Ether o Java

```bash
# Java 21, Ether 9.0.0 (defecto)
mvn archetype:generate ... -DjavaVersion=21 -DetherVersion=9.0.0

# Java 24, Ether 10.0.0
mvn archetype:generate ... -DjavaVersion=24 -DetherVersion=10.0.0
```
