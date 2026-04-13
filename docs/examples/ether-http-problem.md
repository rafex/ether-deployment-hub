# Guía práctica: ether-http-problem

**ether-http-problem** implementa [RFC 7807 Problem Details](https://www.rfc-editor.org/rfc/rfc7807)
para respuestas de error HTTP estructuradas con `Content-Type: application/problem+json`.

## Instalación

```xml
<dependency>
    <groupId>dev.rafex.ether.http.problem</groupId>
    <artifactId>ether-http-problem</artifactId>
    <version>8.0.0-SNAPSHOT</version>
</dependency>
```

---

## `ProblemDetails` — respuesta de error estructurada

```java
// Constructor rápido
ProblemDetails problem = ProblemDetails.of(404, "Not Found", "El usuario 42 no existe");

// Builder completo
ProblemDetails problem = ProblemDetails.builder()
    .type(URI.create("https://errors.example.com/not-found"))
    .title("Not Found")
    .status(404)
    .detail("El usuario con id=42 no existe en el sistema")
    .instance(URI.create("/users/42"))
    .property("userId", 42L)
    .property("timestamp", Instant.now().toString())
    .build();

// Campos del record
URI                    type       = problem.type();
String                 title      = problem.title();
int                    status     = problem.status();
String                 detail     = problem.detail();
URI                    instance   = problem.instance();
Map<String, Object>    properties = problem.properties(); // extensiones
```

Respuesta JSON resultante:

```json
{
  "type": "https://errors.example.com/not-found",
  "title": "Not Found",
  "status": 404,
  "detail": "El usuario con id=42 no existe en el sistema",
  "instance": "/users/42",
  "userId": 42,
  "timestamp": "2025-06-01T14:32:05.123Z"
}
```

---

## `ProblemException` — excepción con problem details

```java
// Lanzar desde cualquier capa del dominio
throw new ProblemException(
    ProblemDetails.of(409, "Conflict", "El email ya está registrado")
);

// Con detalles completos
throw new ProblemException(
    ProblemDetails.builder()
        .status(422)
        .title("Unprocessable Entity")
        .detail("El campo 'email' no tiene formato válido")
        .property("field", "email")
        .property("value", "no-es-un-email")
        .build()
);
```

---

## `ProblemHttpErrorMapper` — mapeo en el pipeline HTTP

Registra el mapper en el pipeline para capturar `ProblemException` automáticamente:

```java
// En el handler de errores de ether-http-core
ErrorMapper mapper = new ProblemHttpErrorMapper(codec);

// Captura ProblemException y emite application/problem+json
Middleware errorHandling = next -> exchange -> {
    try {
        return next.handle(exchange);
    } catch (ProblemException e) {
        ProblemDetails pd = e.problem();
        exchange.problemJson(pd.status(), pd);
        return true;
    }
};
```

---

## Catálogo de errores recomendado

```java
public class Problems {

    public static ProblemException notFound(String resource, Object id) {
        return new ProblemException(ProblemDetails.builder()
            .status(404).title("Not Found")
            .detail(resource + " con id=" + id + " no existe")
            .build());
    }

    public static ProblemException conflict(String detail) {
        return new ProblemException(ProblemDetails.of(409, "Conflict", detail));
    }

    public static ProblemException validationError(String field, String message) {
        return new ProblemException(ProblemDetails.builder()
            .status(422).title("Unprocessable Entity")
            .detail(message)
            .property("field", field)
            .build());
    }

    public static ProblemException unauthorized() {
        return new ProblemException(ProblemDetails.of(401, "Unauthorized",
            "Se requiere autenticación"));
    }
}
```

---

## Más información

- [Guía ether-http-core](ether-http-core.md) — pipeline HTTP y manejo de errores
- [RFC 7807](https://www.rfc-editor.org/rfc/rfc7807) — especificación Problem Details
- [Javadoc API](../api/doxygen/html/index.html)
