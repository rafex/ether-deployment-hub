# Guía práctica: ether-parent

**ether-parent** es el POM raíz y BOM (Bill of Materials) del ecosistema Ether.
Centraliza versiones de dependencias, configuración de plugins y la infraestructura de publicación
para que todos los módulos Ether — y tus propios proyectos — hereden un conjunto coherente de versiones.

## Instalación como POM padre

```xml
<parent>
    <groupId>dev.rafex.ether.parent</groupId>
    <artifactId>ether-parent</artifactId>
    <version>9.6.2</version>
</parent>
```

Al heredar `ether-parent` obtienes automáticamente:
- Configuración del compilador Java 21+
- `maven-flatten-plugin` para publicación limpia
- `maven-source-plugin` y `maven-javadoc-plugin` preconfigurados
- Gestión de versiones de todas las dependencias Ether

## Instalación como BOM (sin herencia)

Si tu proyecto ya tiene otro padre, importa el BOM en `dependencyManagement`:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>dev.rafex.ether.parent</groupId>
            <artifactId>ether-parent</artifactId>
            <version>9.6.2</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Con el BOM importado puedes declarar módulos Ether sin especificar versión:

```xml
<dependencies>
    <dependency>
        <groupId>dev.rafex.ether.di</groupId>
        <artifactId>ether-di</artifactId>
    </dependency>
    <dependency>
        <groupId>dev.rafex.ether.json</groupId>
        <artifactId>ether-json</artifactId>
    </dependency>
    <dependency>
        <groupId>dev.rafex.ether.http.jetty12</groupId>
        <artifactId>ether-http-jetty12</artifactId>
    </dependency>
</dependencies>
```

## Requisitos

- Java 21+
- Maven 3.9+

## Más información

- [Javadoc API](../api/doxygen/html/index.html)
- [Código fuente](https://github.com/rafex/ether-parent)
