# Guía práctica: ether-json

**ether-json** provee serialización y deserialización JSON con una interfaz `JsonCodec`
independiente de la implementación subyacente (Jackson por defecto).

## Instalación

```xml
<dependency>
    <groupId>dev.rafex.ether.json</groupId>
    <artifactId>ether-json</artifactId>
    <version>8.0.0-SNAPSHOT</version>
</dependency>
```

---

## Crear un `JsonCodec`

```java
// Configuración por defecto
JsonCodec codec = JsonCodecBuilder.create().build();

// Modo estricto — falla si hay propiedades desconocidas
JsonCodec strict = JsonCodecBuilder.strict().build();

// Modo permisivo
JsonCodec lenient = JsonCodecBuilder.lenient().build();

// Configuración personalizada
JsonCodec custom = JsonCodecBuilder.create()
    .failOnUnknownProperties(false)
    .writeDatesAsTimestamps(false)
    .prettyPrint(false)
    .build();
```

---

## Serialización

```java
record User(long id, String name, String email) {}

User user = new User(1L, "Alice", "alice@example.com");

// A String
String json = codec.toJson(user);
// → {"id":1,"name":"Alice","email":"alice@example.com"}

// Formato legible
String pretty = codec.toPrettyJson(user);

// A bytes (para HTTP response)
byte[] bytes = codec.toJsonBytes(user);

// A OutputStream
try (OutputStream out = Files.newOutputStream(path)) {
    codec.writeValue(out, user);
}
```

---

## Deserialización

```java
// Desde String
User user = codec.readValue(json, User.class);

// Desde bytes
User user = codec.readValue(bytes, User.class);

// Desde InputStream
try (InputStream in = Files.newInputStream(path)) {
    User user = codec.readValue(in, User.class);
}

// Con genéricos — TypeReference
List<User> users = codec.readValue(json, new TypeReference<List<User>>() {});
Map<String, Object> map = codec.readValue(json, new TypeReference<Map<String, Object>>() {});
```

---

## Árbol de nodos (JsonNode)

```java
JsonNode root = codec.readTree(json);

String name  = root.get("name").asText();
int    age   = root.get("age").asInt();
boolean hasX = root.has("optionalField");

// Convertir nodo a objeto
User user = codec.treeToValue(root, User.class);
```

---

## Integración con ether-di

```java
public class AppContainer {

    private final Lazy<JsonCodec> json = new Lazy<>(() ->
            JsonCodecBuilder.create().writeDatesAsTimestamps(false).build());

    public JsonCodec json() { return json.get(); }
}
```

---

## Uso típico en endpoints HTTP

```java
// Deserializar body de la request
CreateUserRequest req = codec.readValue(exchange.bodyBytes(), CreateUserRequest.class);

// Serializar respuesta
User created = userService.create(req);
exchange.json(201, created); // ether-http-core lo serializa automáticamente
```

---

## Más información

- [Javadoc API](../api/doxygen/html/index.html)
- [Código fuente](https://github.com/rafex/ether-json)
