# Guía práctica: ether-http-core

**ether-http-core** define los contratos del pipeline HTTP de Ether: `HttpExchange`,
`HttpHandler`, `Middleware`, `HttpResource` y `Route`. Es independiente del servidor HTTP concreto.

## Instalación

```xml
<dependency>
    <groupId>dev.rafex.ether.http.core</groupId>
    <artifactId>ether-http-core</artifactId>
    <version>8.0.0-SNAPSHOT</version>
</dependency>
```

---

## `HttpResource` — handler por verbo HTTP

Implementa `HttpResource` para exponer un recurso REST:

```java
public class UsersResource implements HttpResource {

    private final UserService userService;
    private final JsonCodec   codec;

    public UsersResource(UserService userService, JsonCodec codec) {
        this.userService = userService;
        this.codec = codec;
    }

    @Override
    public boolean get(HttpExchange x) {
        List<User> users = userService.findAll();
        x.json(200, users);
        return true;
    }

    @Override
    public boolean post(HttpExchange x) {
        CreateUserRequest req = codec.readValue(x.bodyBytes(), CreateUserRequest.class);
        User created = userService.create(req);
        x.json(201, created);
        return true;
    }

    @Override
    public Set<String> supportedMethods() {
        return Set.of("GET", "POST");
    }
}
```

---

## `HttpExchange` — contexto de la petición

```java
// Método y path
String method = exchange.method();  // "GET", "POST", …
String path   = exchange.path();    // "/users/123"

// Parámetros de path (definidos en Route)
String userId = exchange.pathParam("id"); // "123"
Map<String, String> allPath = exchange.pathParams();

// Query string
String sort  = exchange.queryFirst("sort");          // "name"
List<String> tags = exchange.queryAll("tag");        // ["java","api"]
Map<String, List<String>> all = exchange.queryParams();

// Respuestas
exchange.json(200, body);           // Content-Type: application/json
exchange.text(200, "OK");           // Content-Type: text/plain
exchange.noContent(204);            // sin body
exchange.methodNotAllowed();        // 405 con Allow header
exchange.options();                 // 200 con métodos permitidos
```

---

## `Route` — patrón de URL

```java
// Ruta estática
Route r1 = Route.of("/users", Set.of("GET", "POST"));

// Ruta con parámetro
Route r2 = Route.of("/users/{id}", Set.of("GET", "PUT", "DELETE"));

// Verificar método
boolean ok = r2.allows("GET"); // true

// Extraer parámetros
Optional<Map<String, String>> params = r2.match("/users/42");
// → Optional.of({"id": "42"})
```

---

## `Middleware` — composición de interceptores

```java
// Middleware de logging
Middleware logging = next -> exchange -> {
    LOG.info("{} {}", exchange.method(), exchange.path());
    boolean handled = next.handle(exchange);
    LOG.info("→ completado");
    return handled;
};

// Middleware de autenticación
Middleware auth = next -> exchange -> {
    String token = exchange.headerFirst("Authorization");
    if (token == null) { exchange.noContent(401); return true; }
    // verificar...
    return next.handle(exchange);
};

// Composición: auth → logging → handler
HttpHandler pipeline = auth.wrap(logging.wrap(resource::handle));
```

---

## `QuerySpec` y `RsqlParser` — filtrado y paginación

```java
// Parsear filtro RSQL desde query param
// GET /users?filter=role==ADMIN;active==true&sort=name,asc&page=0&size=20
QuerySpec spec = QuerySpec.fromRequest(exchange);

String rsql = spec.filter();  // "role==ADMIN;active==true"
Sort   sort = spec.sort();    // Sort.by("name", ASC)
int    page = spec.page();    // 0
int    size = spec.size();    // 20

// Parsear el árbol RSQL
RsqlNode tree = RsqlParser.parse(rsql);
```

---

## `AuthPolicy` — política de autenticación/autorización

```java
public class JwtAuthPolicy implements AuthPolicy {

    private final TokenVerifier verifier;

    @Override
    public boolean authenticate(HttpExchange exchange) {
        String header = exchange.headerFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return false;
        String token = header.substring(7);
        VerificationResult result = verifier.verify(token, Instant.now());
        if (!result.isValid()) return false;
        exchange.attribute("claims", result.claims());
        return true;
    }

    @Override
    public boolean authorize(HttpExchange exchange, String... requiredRoles) {
        TokenClaims claims = (TokenClaims) exchange.attribute("claims");
        return claims.roles().containsAll(List.of(requiredRoles));
    }
}
```

---

## Más información

- [Guía ether-http-jetty12](ether-http-jetty12.md) — servidor HTTP con Jetty 12
- [Guía ether-http-security](ether-http-security.md) — CORS, rate limit, headers de seguridad
- [Javadoc API](../api/doxygen/html/index.html)
