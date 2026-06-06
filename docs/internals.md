# Funcionamiento interno del arquetipo

Este documento explica cómo está construido el arquetipo `ether-hexagonal-archetype` y los detalles técnicos que no son obvios al usarlo.

---

## Estructura del módulo Maven del arquetipo

```
ether-archetype/
├── pom.xml                          ← packaging: maven-archetype
├── src/
│   └── main/
│       └── resources/
│           ├── META-INF/
│           │   ├── maven/
│           │   │   └── archetype-metadata.xml   ← descriptor del arquetipo
│           │   └── archetype-post-generate.groovy ← script post-generación
│           └── archetype-resources/             ← plantillas del proyecto generado
│               ├── pom.xml
│               ├── Makefile
│               ├── Justfile
│               ├── mvnw / mvnw.cmd
│               ├── .mvn/
│               ├── config/
│               └── __rootArtifactId__-{modulo}/
```

---

## El motor de plantillas: Velocity

Maven Archetype Plugin usa **Apache Velocity** para procesar las plantillas. Cuando generas un proyecto, Velocity lee cada archivo de `archetype-resources/` y sustituye las variables.

### Variables disponibles

| Variable               | Descripción                               | Ejemplo                   |
|------------------------|-------------------------------------------|---------------------------|
| `${groupId}`           | Group ID del proyecto                     | `com.example`             |
| `${artifactId}`        | Artifact ID base                          | `myapp`                   |
| `${rootArtifactId}`    | Igual que `artifactId`                    | `myapp`                   |
| `${version}`           | Versión del proyecto                      | `0.1.0-SNAPSHOT`          |
| `${package}`           | Paquete raíz Java                         | `com.example.myapp`       |
| `${packageInPathFormat}` | Paquete como ruta de directorios        | `com/example/myapp`       |
| `${javaVersion}`       | Propiedad custom del arquetipo            | `21`                      |
| `${etherVersion}`      | Propiedad custom del arquetipo            | `9.0.0`                   |

### Tokens de directorio

Para nombrar directorios, Velocity usa una sintaxis especial con doble guión bajo:

| Token                   | Se reemplaza por                   | Ejemplo                  |
|-------------------------|------------------------------------|--------------------------|
| `__rootArtifactId__`    | El valor de `artifactId`           | `myapp`                  |
| `__packageInPathFormat__` | El paquete como ruta             | `com/example/myapp`      |
| `__artifactId__`        | Igual que `__rootArtifactId__`     | `myapp`                  |

Esto hace que el directorio `__rootArtifactId__-core/` se genere como `myapp-core/`.

---

## `archetype-metadata.xml`: el descriptor

Este archivo le dice al plugin qué módulos y archivos componen el arquetipo.

### Estructura general

```xml
<archetype-descriptor name="${rootArtifactId}" partial="false">

  <requiredProperties>
    <!-- propiedades que el usuario debe proporcionar o tienen valor por defecto -->
  </requiredProperties>

  <modules>
    <!-- módulos Maven del proyecto generado -->
  </modules>

  <fileSets>
    <!-- archivos en la raíz del proyecto generado -->
  </fileSets>

</archetype-descriptor>
```

### Propiedades requeridas

```xml
<requiredProperty key="javaVersion">
    <defaultValue>21</defaultValue>
</requiredProperty>
```

Estas propiedades se añaden a las estándar de Maven (`groupId`, `artifactId`, `version`, `package`) y quedan disponibles como variables Velocity (`${javaVersion}`).

### Declaración de módulos

```xml
<module id="${rootArtifactId}-core"
        dir="__rootArtifactId__-core"
        name="${rootArtifactId}-core">
    <fileSets>
        <fileSet filtered="true" packaged="false" encoding="UTF-8">
            <directory>src/main/java</directory>
            <includes>
                <include>**/*.java</include>
            </includes>
        </fileSet>
    </fileSets>
</module>
```

**Atributos críticos del fileSet:**

- `filtered="true"` — activa el procesado Velocity (variables `${...}` sustituidas)
- `filtered="false"` — copia el archivo tal cual (scripts, binarios, JARs)
- `packaged="true"` — **agrega** el paquete como subcarpeta (ej. `com/example/myapp/`)
- `packaged="false"` — NO agrega el paquete (la ruta del directorio ya lo incluye)

### ¿Por qué `packaged="false"` en los fileSet de Java?

El directorio de las plantillas ya contiene el token `__packageInPathFormat__`:

```
src/main/java/__packageInPathFormat__/services/ExampleService.java
```

Velocity expande `__packageInPathFormat__` a `com/example/myapp`. Si además se usara `packaged="true"`, el plugin añadiría *otra vez* el paquete, generando:

```
src/main/java/com/example/myapp/com/example/myapp/services/ExampleService.java  ← INCORRECTO
```

Con `packaged="false"`:

```
src/main/java/com/example/myapp/services/ExampleService.java  ← CORRECTO
```

---

## `archetype-post-generate.groovy`: el script post-generación

Se ejecuta automáticamente al finalizar la generación del proyecto. Es la solución a dos limitaciones del plugin.

### Limitación 1: archivos que empiezan con punto (`.`)

El maven-archetype-plugin **ignora silenciosamente** todos los archivos cuyo nombre comienza con `.` (ej. `.gitignore`, `.env.example`). No lanza error; simplemente no los incluye en el proyecto generado.

**Solución:** crear los archivos programáticamente desde Groovy:

```groovy
def projectDir = new File(request.outputDirectory, request.artifactId)

new File(projectDir, ".gitignore").text = """\
target/
.flattened-pom.xml
...
"""
```

### Limitación 2: permisos de archivo

El plugin no preserva el bit de ejecución de `mvnw`. En Unix, después de la generación el script no es ejecutable.

**Solución:**

```groovy
def mvnw = new File(projectDir, "mvnw")
if (mvnw.exists()) {
    mvnw.setExecutable(true, false)  // ejecutable por todos los usuarios
}
```

### Variables disponibles en el script

| Variable            | Tipo   | Descripción                                     |
|---------------------|--------|-------------------------------------------------|
| `request`           | Object | Objeto con todos los parámetros de generación   |
| `request.outputDirectory` | String | Directorio donde se generó el proyecto  |
| `request.artifactId` | String | `artifactId` proporcionado por el usuario       |
| `request.groupId`   | String | `groupId`                                       |
| `request.version`   | String | `version`                                       |
| `request.properties` | Map   | Todas las propiedades (incluyendo las custom)   |

---

## Archivos estáticos vs. filtrados

### `filtered="false"` (copia exacta)

- `mvnw` / `mvnw.cmd` — scripts shell; `${VAR}` en ellos es sintaxis de bash, NO de Velocity
- `maven-wrapper.properties` — no tiene variables Velocity
- `config/checkstyle/` y `config/owasp/` — XML estático

Si `filtered="true"` se aplicara a `mvnw`, Velocity intentaría sustituir las referencias `${JAVA_HOME}` del script y las rompería.

### `filtered="true"` (procesado por Velocity)

- Todos los `.java` — contienen `${package}`, `${rootArtifactId}`, etc.
- `pom.xml` de cada módulo
- `Makefile` y `Justfile`
- `config/spotless/` — contiene el año de `${inceptionYear}`

---

## Velocity y caracteres especiales

### `##` — comentario de Velocity

El motor Velocity interpreta `##` como inicio de comentario de línea. Esto causó un problema con los comentarios de ayuda del Makefile:

```makefile
build: ## Compile all modules  ← Velocity elimina "## Compile all modules"
```

**Solución:** usar `@echo` explícito en el target `help`:

```makefile
help:
    @echo "  build         Compile all modules"
```

### `${...}` en scripts shell

Las variables de shell como `${JAVA_HOME}` o `${1:-default}` se deben evitar en archivos `filtered="true"`. Opciones:

1. Marcar el archivo como `filtered="false"` (para mvnw).
2. Usar `$VARIABLE` sin llaves donde sea posible.
3. En Justfile, usar `env_var_or_default("KEY", "default")` en lugar de `${KEY:-default}`.

### `{{...}}` — sintaxis de Just

Justfile usa `{{variable}}` para sus propias variables. Velocity no interfiere con `{{}}`, por lo que es seguro.

---

## Construir y publicar el arquetipo

### Build local

```bash
cd ether-archetype
./mvnw clean install
```

Instala `dev.rafex.ether:ether-hexagonal-archetype:1.0.0` en `~/.m2/repository`.

### Verificar el arquetipo generado

```bash
./mvnw archetype:crawl
# actualiza el catálogo local de arquetipos
```

### Publicar en repositorio remoto

Configura el `<distributionManagement>` en el `pom.xml` del arquetipo y ejecuta:

```bash
./mvnw clean deploy
```

---

## Relación con `ether-deployment-hub`

`ether-archetype` vive como un **submódulo git** dentro de `ether-deployment-hub`:

```
ether-deployment-hub/
└── ether-archetype/   ← submódulo git (repo independiente)
```

Comandos útiles:

```bash
# actualizar el submódulo al último commit
git submodule update --remote ether-archetype

# clonar incluyendo submódulos
git clone --recurse-submodules https://github.com/rafex/ether-deployment-hub
```

---

## Esquema de versionado del arquetipo

| Versión archetype | Versión Ether defecto | Java mínimo |
|-------------------|----------------------|-------------|
| 1.0.0             | 9.0.0                | 21          |

El arquetipo en sí tiene su propia versión independiente de Ether. El parámetro `etherVersion` controla qué versión del framework se usa en el proyecto generado.
