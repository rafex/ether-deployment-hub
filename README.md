# Ether Central Publishing Hub

![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/rafex/ether-central-publishing-hub/publish.yml?branch=main)

## Objetivo del Repositorio

Este repositorio actúa como un hub orquestador para la publicación y despliegue automáticos de todos los módulos de la biblioteca **Ether** en **Maven Central**. Incluye:
- Scripts y plantillas de configuración para generación de artefactos (Javadoc, fuentes, firmas GPG).
- Workflows de GitHub Actions preconfigurados para ejecutar `mvn deploy` usando el plugin `central-publishing-maven-plugin`.
- Gestión centralizada de credenciales y versiones de cada módulo.
- Ejemplos y documentación paso a paso para asegurar despliegues consistentes, seguros y reproducibles en el repositorio central de Maven.



## Acerca de la biblioteca Ether

Este repositorio orquesta el despliegue de **Ether**, una colección de módulos Java para crear servicios REST ligeros sin depender de frameworks pesados como Spring Boot. Ether está comprobado en producción y ofrece componentes modulares y autónomos.

### Componentes principales

- **ether-cli**: Utilidades de línea de comandos.
- **ether-email**: Envío y gestión de correos.
- **ether-jdbc**: Conexión y manejo de bases de datos via JDBC.
- **ether-json**: Validación y manipulación de JSON.
- **ether-jwt**: Autenticación basada en JSON Web Tokens.
- **ether-object**: Mapeo y conversión de objetos.
- **ether-parent**: POM padre con configuración común.
- **ether-properties**: Lectura y gestión de archivos de propiedades.
- **ether-rest**: Cliente y servidor REST minimalista.

### Cómo compilar y publicar

1. Compila con Maven:
   ```bash
   mvn clean compile
   ```
2. Publica cada módulo con Maven Central desde este hub, usando GitHub Actions.
3. Para usar Ether en tu proyecto, agrega en tu POM:
   ```xml
   <parent>
     <groupId>dev.rafex.ether.parent</groupId>
     <artifactId>ether-parent</artifactId>
     <version>2.0.0</version>
     <relativePath/>
   </parent>
   ```
   Y luego añade el módulo deseado:
   ```xml
   <dependency>
     <groupId>dev.rafex.ether.rest</groupId>
     <artifactId>ether-rest</artifactId>
     <version>2.0.0</version>
   </dependency>
   ```
