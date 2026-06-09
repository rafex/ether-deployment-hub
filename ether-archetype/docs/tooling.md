# Tooling: Make, Just y Maven Wrapper

## Filosofía de separación de responsabilidades

El proyecto generado usa **tres herramientas** con responsabilidades distintas y una regla clara de precedencia:

```
Maven Wrapper (./mvnw)
      ▲
      │ invoca
    Make                ← build system: compila, testea, empaqueta, calidad
      ▲
      │ puede invocar
     Just               ← task runner: ejecuta, docker, bd, ciclo de desarrollo
```

**Regla fundamental:** `just` puede llamar a `make`; `make` NUNCA puede llamar a `just`.

Esta separación garantiza que:
- El sistema de build sea determinista y scriptable sin instalar `just`
- Las tareas de desarrollo (runtime, docker, bd) no contaminen el proceso de build
- En CI se puede usar sólo `make` sin necesidad de `just`

---

## Maven Wrapper (`./mvnw`)

### ¿Qué es?

El Maven Wrapper es un script de shell (`mvnw` en Unix, `mvnw.cmd` en Windows) que descarga automáticamente la versión exacta de Maven especificada en `.mvn/wrapper/maven-wrapper.properties`. Permite que cualquier desarrollador clone el repo y compile sin tener Maven instalado globalmente.

### Configuración

```properties
# .mvn/wrapper/maven-wrapper.properties
wrapperVersion=3.3.1
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.12/apache-maven-3.9.12-bin.zip
```

### Uso

```bash
./mvnw compile        # en lugar de mvn compile
./mvnw test
./mvnw clean install
```

### Permisos

El script `mvnw` necesita permiso de ejecución. El arquetipo lo concede automáticamente via `archetype-post-generate.groovy`:

```groovy
def mvnw = new File(projectDir, "mvnw")
if (mvnw.exists()) {
    mvnw.setExecutable(true, false)
}
```

Si clonas desde git y el permiso se pierde:

```bash
chmod +x mvnw
```

### `.gitignore` del wrapper

```gitignore
# .mvn/wrapper/.gitignore
maven-wrapper.jar
MavenWrapperDownloader.java
```

El JAR del wrapper se descarga automáticamente; no se versiona.

---

## Makefile (build system)

### Responsabilidad

`make` gestiona el **ciclo de compilación y calidad**: compilar, testear, empaquetar y verificar. No sabe nada de cómo ejecutar la aplicación.

### Targets disponibles

| Target         | Comando Maven equivalente                                        | Descripción                          |
|----------------|------------------------------------------------------------------|--------------------------------------|
| `help`         | —                                                                | Lista todos los targets              |
| `build`        | `./mvnw compile`                                                 | Compila todos los módulos            |
| `test`         | `./mvnw test`                                                    | Ejecuta los tests (incluye ArchUnit) |
| `clean`        | `./mvnw clean`                                                   | Elimina carpetas `target/`           |
| `format`       | `./mvnw spotless:apply`                                          | Aplica formato automático            |
| `format-check` | `./mvnw spotless:check`                                          | Verifica formato (sin modificar)     |
| `quality`      | `./mvnw -Pquality verify`                                        | Spotless + Checkstyle + OWASP        |
| `install`      | `./mvnw clean install -DskipTests`                               | Instala en repo local (sin tests)    |
| `install-full` | `./mvnw clean install`                                           | Instala con tests                    |
| `package`      | `./mvnw clean package -pl {app}-transport-jetty -am`             | Fat-jar de transport-jetty           |

### Uso

```bash
make          # muestra ayuda
make build
make test
make quality
```

---

## Justfile (task runner)

### Responsabilidad

`just` gestiona el **ciclo de vida del desarrollador**: ejecutar la aplicación, interactuar con Docker, la base de datos y otras tareas que requieren el entorno local.

### Instalar just

```bash
# macOS / Linux via Homebrew
brew install just

# Linux via cargo
cargo install just

# ver más opciones en:
# https://just.systems/man/en/packages.html
```

### Variables configurables

| Variable  | Defecto                                         | Descripción                   |
|-----------|-------------------------------------------------|-------------------------------|
| `app`     | `{rootArtifactId}` (ej. `myapp`)               | Nombre base de la aplicación  |
| `version` | `{version}` (ej. `0.1.0-SNAPSHOT`)             | Versión del proyecto          |
| `jar`     | ruta al fat-jar de transport-jetty              | JAR a ejecutar                |
| `image`   | `{app}:{version}` (ej. `myapp:0.1.0-SNAPSHOT`) | Nombre de la imagen Docker    |
| `port`    | `$PORT` o `8080`                                | Puerto de la aplicación       |

### Recipes disponibles

#### Dev lifecycle

| Recipe        | Descripción                                              |
|---------------|----------------------------------------------------------|
| `run`         | Compila (via make package) y ejecuta el fat-jar          |
| `run-debug`   | Igual que `run` pero con `--log=DEBUG`                   |
| `run-log NIVEL` | Ejecuta con el nivel de log especificado              |
| `watch`       | Recompila y reinicia al detectar cambios `.java` (requiere `entr`) |

#### Docker

| Recipe          | Descripción                                          |
|-----------------|------------------------------------------------------|
| `docker-build`  | Construye la imagen Docker (requiere `Dockerfile`)   |
| `docker-run`    | Ejecuta el contenedor (lee `.env` si existe)         |
| `up`            | `docker compose up -d`                               |
| `down`          | `docker compose down`                                |
| `restart [svc]` | Reinicia un servicio específico (defecto: `app`)     |
| `logs [svc]`    | Sigue los logs de un servicio (defecto: `app`)       |

#### Base de datos

| Recipe     | Descripción                                                     |
|------------|-----------------------------------------------------------------|
| `migrate`  | Placeholder para Flyway/Liquibase                               |
| `seed`     | Placeholder para datos de desarrollo                            |
| `db-shell` | Abre `psql` con `$DB_URL` o `postgresql://localhost:5432/appdb` |

#### CI

| Recipe     | Descripción                           |
|------------|---------------------------------------|
| `ci`       | `make quality` (checks + tests)       |
| `security` | `make quality` (incluye OWASP)        |

#### Delegación a Make

| Recipe        | Make equivalente     |
|---------------|----------------------|
| `build`       | `make build`         |
| `test`        | `make test`          |
| `clean`       | `make clean`         |
| `fmt`         | `make format`        |
| `fmt-check`   | `make format-check`  |
| `install`     | `make install`       |

### Uso

```bash
just              # lista todos los recipes
just run          # ejecuta la aplicación
just run-log WARN # con nivel WARN
just up           # levanta docker compose
just db-shell     # abre psql
```

---

## Calidad de código

### Spotless (formato)

Configurado con el formateador de Eclipse 4.29. Garantiza estilo uniforme en todo el proyecto.

**Orden de imports:**

```
{package}   ← imports del propio proyecto primero
java
javax
jakarta
org
com
```

**Cabecera de licencia:** configurada en `config/spotless/license-header.txt`. Spotless la agrega/verifica automáticamente en cada `.java`.

```bash
make format         # aplica formato
make format-check   # verifica (falla si hay diferencias)
```

### Checkstyle

Reglas definidas en `config/checkstyle/checkstyle.xml` con supresiones en `config/checkstyle/suppressions.xml`.

Se ejecuta en la fase `verify` del perfil `quality`:

```bash
make quality
```

### OWASP Dependency Check

Escanea las dependencias del proyecto contra la base de datos NVD buscando CVEs conocidos.

Requiere una API key de NVD (gratuita):

```bash
export NVD_API_KEY=tu-api-key
make quality
```

Los reportes se generan en `target/dependency-check-report.html` (en cada módulo).

Configuración en `config/owasp/dependency-check-suppressions.xml` para suprimir falsos positivos.

**CVSS threshold:** el build falla si alguna dependencia tiene una vulnerabilidad con CVSS ≥ 11 (es decir, nunca falla por defecto; ajusta `failBuildOnCVSS` en el POM padre si quieres un umbral más estricto).

### ArchUnit

Verifica la arquitectura hexagonal en tiempo de compilación (`mvn test`). No requiere configuración adicional.

Ver [architecture.md](architecture.md) para las reglas implementadas.

---

## Perfil Maven `quality`

El POM padre define un perfil `quality` que activa los checks en la fase `verify`:

```bash
./mvnw -Pquality verify
# equivalente a: make quality
```

En builds normales (`mvn compile`, `mvn test`), los checks de Spotless, Checkstyle y OWASP **no se ejecutan**. Esto mantiene el ciclo de desarrollo rápido.

| Fase       | Build normal | Build con `-Pquality` |
|------------|-------------|----------------------|
| compile    | ✅ sí        | ✅ sí                 |
| test       | ✅ sí        | ✅ sí                 |
| spotless   | ❌ no        | ✅ sí (validate)      |
| checkstyle | ❌ no        | ✅ sí (verify)        |
| owasp      | ❌ no        | ✅ sí (verify)        |
