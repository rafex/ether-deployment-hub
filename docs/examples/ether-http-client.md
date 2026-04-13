# Guía práctica: ether-http-client

**ether-http-client** provee un cliente HTTP tipado sobre `java.net.http.HttpClient` (Java 21+)
con DSL fluente, deserialización JSON integrada y soporte para todos los verbos HTTP.

## Instalación

```xml
<dependency>
    <groupId>dev.rafex.ether.http.client</groupId>
    <artifactId>ether-http-client</artifactId>
    <version>8.0.0-SNAPSHOT</version>
</dependency>
```

---

## Crear el cliente

```java
// Con configuración por defecto
EtherHttpClient client = new DefaultEtherHttpClient(config);

// Con HttpClient personalizado
HttpClient httpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(10))
    .followRedirects(HttpClient.Redirect.NORMAL)
    .build();
EtherHttpClient client = new DefaultEtherHttpClient(httpClient, codec);
```

---

## GET

```java
// Shortcut GET
HttpResponseSpec response = client.get(URI.create("https://api.example.com/users"));

int    status = response.status();           // 200
String body   = response.bodyAsString();     // JSON string
byte[] bytes  = response.body();             // bytes crudos

// Deserializar directamente
List<User> users = client.sendJson(
    HttpRequestSpec.get(URI.create("https://api.example.com/users")).build(),
    new TypeReference<List<User>>() {}
);
```

---

## POST con JSON

```java
CreateUserRequest req = new CreateUserRequest("Alice", "alice@example.com");

HttpResponseSpec response = client.send(
    HttpRequestSpec.post(URI.create("https://api.example.com/users"))
        .jsonBody(req)           // serializa con JsonCodec
        .header("Authorization", "Bearer " + token)
        .timeout(Duration.ofSeconds(30))
        .build()
);

if (response.status() == 201) {
    User created = response.bodyAsJson(User.class);
}
```

---

## PUT, PATCH, DELETE

```java
// PUT
client.send(
    HttpRequestSpec.put(URI.create("https://api.example.com/users/42"))
        .jsonBody(updateRequest)
        .build()
);

// PATCH
client.send(
    HttpRequestSpec.patch(URI.create("https://api.example.com/users/42"))
        .jsonBody(patchRequest)
        .build()
);

// DELETE
HttpResponseSpec del = client.send(
    HttpRequestSpec.delete(URI.create("https://api.example.com/users/42"))
        .header("Authorization", "Bearer " + token)
        .build()
);
assert del.status() == 204;
```

---

## Cabeceras de respuesta

```java
HttpResponseSpec response = client.get(URI.create("https://api.example.com/users"));

Map<String, List<String>> headers = response.headers();
String contentType = response.headers().get("content-type").get(0);
String requestId   = response.headers().getOrDefault("x-request-id", List.of("")).get(0);
```

---

## `HttpMethod` enum

```java
HttpMethod.GET
HttpMethod.POST
HttpMethod.PUT
HttpMethod.PATCH
HttpMethod.DELETE
HttpMethod.HEAD
HttpMethod.OPTIONS
```

---

## Integración con ether-di

```java
public class AppContainer implements AutoCloseable {

    private final Closer closer = new Closer();

    private final Lazy<EtherHttpClient> httpClient = new Lazy<>(() ->
            closer.register(new DefaultEtherHttpClient(
                HttpClientConfig.defaults(), codec.get())));

    public EtherHttpClient httpClient() { return httpClient.get(); }

    @Override public void close() { closer.close(); }
}
```

---

## Más información

- [Guía ether-webhook](ether-webhook.md) — envío de webhooks usando `EtherHttpClient`
- [Javadoc API](../api/doxygen/html/index.html)
