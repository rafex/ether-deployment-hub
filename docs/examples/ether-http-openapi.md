# Guía práctica: ether-http-openapi

**ether-http-openapi** permite construir y exponer una especificación OpenAPI 3.x programáticamente
usando records Java inmutables, sin anotaciones ni procesadores de anotaciones.

## Instalación

```xml
<dependency>
    <groupId>dev.rafex.ether.http.openapi</groupId>
    <artifactId>ether-http-openapi</artifactId>
    <version>8.0.0-SNAPSHOT</version>
</dependency>
```

---

## Construir un documento OpenAPI

```java
OpenApiDocument doc = OpenApiDocumentBuilder.create()
    .info(new OpenApiInfo("Mi API", "1.0.0", "API de ejemplo con Ether"))
    .server(new OpenApiServer("https://api.example.com", "Producción"))
    .server(new OpenApiServer("http://localhost:8080", "Desarrollo"))
    .path("/users", pathItem -> pathItem
        .get(op -> op
            .operationId("listUsers")
            .summary("Listar usuarios")
            .tag("users")
            .response("200", resp -> resp
                .description("Lista de usuarios")
                .json(schema -> schema.array().items(schema.ref("User")))))
        .post(op -> op
            .operationId("createUser")
            .summary("Crear usuario")
            .tag("users")
            .requestBody(body -> body.required(true).json(schema -> schema.ref("CreateUserRequest")))
            .response("201", resp -> resp
                .description("Usuario creado")
                .json(schema -> schema.ref("User")))
            .response("422", resp -> resp.description("Error de validación"))))
    .path("/users/{id}", pathItem -> pathItem
        .parameter(param -> param.name("id").in("path").required(true).schema(s -> s.type("integer")))
        .get(op -> op
            .operationId("getUser")
            .summary("Obtener usuario por ID")
            .response("200", resp -> resp.json(schema -> schema.ref("User")))
            .response("404", resp -> resp.description("Usuario no encontrado"))))
    .component("User", schema -> schema
        .type("object")
        .property("id", s -> s.type("integer").format("int64"))
        .property("name", s -> s.type("string"))
        .property("email", s -> s.type("string").format("email")))
    .component("CreateUserRequest", schema -> schema
        .type("object").required("name", "email")
        .property("name", s -> s.type("string"))
        .property("email", s -> s.type("string").format("email")))
    .build();

// Serializar
String json   = doc.toJson();
String pretty = doc.toPrettyJson();
```

---

## Exponer en un endpoint HTTP

```java
public class OpenApiResource implements HttpResource {

    private final String specJson;

    public OpenApiResource(OpenApiDocument doc) {
        this.specJson = doc.toPrettyJson();
    }

    @Override
    public boolean get(HttpExchange x) {
        x.json(200, specJson);
        return true;
    }

    @Override
    public Set<String> supportedMethods() { return Set.of("GET"); }
}
```

Registrar la ruta en Jetty:

```java
// GET /openapi.json → devuelve la especificación
Route.of("/openapi.json", Set.of("GET"))
```

---

## Seguridad — Bearer JWT

```java
OpenApiDocumentBuilder.create()
    .securityScheme("bearerAuth", scheme -> scheme
        .type("http")
        .scheme("bearer")
        .bearerFormat("JWT"))
    .security("bearerAuth")  // aplica globalmente
    // ...
```

---

## Integración con ether-di

```java
public class AppContainer {

    private final Lazy<OpenApiDocument> openApi = new Lazy<>(() ->
            OpenApiDocumentBuilder.create()
                .info(new OpenApiInfo("Mi API", "1.0.0", ""))
                // ... rutas ...
                .build());

    public OpenApiDocument openApiDocument() { return openApi.get(); }
}
```

---

## Más información

- [Guía ether-http-core](ether-http-core.md) — `HttpResource` y routing
- [Guía ether-http-jetty12](ether-http-jetty12.md) — registro de rutas en Jetty
- [Javadoc API](../api/doxygen/html/index.html)
