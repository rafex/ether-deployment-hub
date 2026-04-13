# Guía práctica: ether-webhook

**ether-webhook** provee firma HMAC y verificación de webhooks con `WebhookSigner`,
`WebhookVerifier` y un cliente de entrega `WebhookDeliveryClient`.

## Instalación

```xml
<dependency>
    <groupId>dev.rafex.ether.webhook</groupId>
    <artifactId>ether-webhook</artifactId>
    <version>8.0.0-SNAPSHOT</version>
</dependency>
```

---

## `WebhookPayload` — construir el payload

```java
// JSON — serializa el objeto automáticamente
WebhookPayload payload = WebhookPayload.ofJson(
    UUID.randomUUID().toString(),   // deliveryId
    "user.created",                 // eventType
    new UserCreatedEvent(42L, "Alice", "alice@example.com")
);

// Texto plano
WebhookPayload textPayload = WebhookPayload.ofText(
    UUID.randomUUID().toString(),
    "order.shipped",
    "Pedido #123 enviado el 2025-06-01"
);

// Añadir cabeceras HTTP
payload = payload
    .withHeader("X-Api-Version", "2")
    .withHeader("X-Source", "my-app");

// Campos del payload
String              deliveryId  = payload.deliveryId();
String              eventType   = payload.eventType();
Instant             occurredAt  = payload.occurredAt();
byte[]              body        = payload.body();
Map<String,List<String>> headers = payload.headers();

// Convertir a HttpRequestSpec para enviar
HttpRequestSpec req = payload.toRequest(URI.create("https://listener.example.com/webhook"));
```

---

## `WebhookSigner` — firmar payloads (HMAC)

```java
byte[] secret = System.getenv("WEBHOOK_SECRET").getBytes(StandardCharsets.UTF_8);
WebhookSigner signer = new HmacWebhookSignerVerifier(secret);

WebhookSignature signature = signer.sign(payload);
String sigValue = signature.value(); // "sha256=abc123..."
String sigHeader = signature.headerName(); // "X-Hub-Signature-256"
```

---

## `WebhookVerifier` — verificar firmas recibidas

```java
byte[] secret = System.getenv("WEBHOOK_SECRET").getBytes(StandardCharsets.UTF_8);
WebhookVerifier verifier = new HmacWebhookSignerVerifier(secret);

// En el endpoint receptor
WebhookPayload  received  = buildPayloadFromRequest(httpExchange);
WebhookSignature incomingSig = WebhookSignature.of(
    exchange.headerFirst("X-Hub-Signature-256")
);

WebhookVerificationResult result = verifier.verify(received, incomingSig);

if (!result.isValid()) {
    exchange.noContent(401);
    return true;
}

// Procesar el webhook
processEvent(received.eventType(), received.body());
exchange.noContent(200);
return true;
```

---

## `WebhookDeliveryClient` — enviar webhooks

```java
WebhookDeliveryClient deliveryClient = new WebhookDeliveryClient(
    httpClient,  // EtherHttpClient
    signer       // WebhookSigner — firma automáticamente
);

// Enviar con reintento automático
deliveryClient.deliver(
    URI.create("https://customer.example.com/webhooks"),
    payload
);
```

---

## `WebhookHeaders` — cabeceras estándar

```java
WebhookHeaders.DELIVERY_ID         // "X-Delivery-Id"
WebhookHeaders.EVENT_TYPE          // "X-Event-Type"
WebhookHeaders.SIGNATURE_SHA256    // "X-Hub-Signature-256"
WebhookHeaders.TIMESTAMP           // "X-Timestamp"
```

---

## Patrón productor de webhooks con ether-di

```java
public class AppContainer {

    private final Lazy<WebhookSigner> signer = new Lazy<>(() ->
            new HmacWebhookSignerVerifier(
                config.get().require("webhook.secret").getBytes(StandardCharsets.UTF_8)
            ));

    private final Lazy<WebhookDeliveryClient> webhookClient = new Lazy<>(() ->
            new WebhookDeliveryClient(httpClient.get(), signer.get()));

    public WebhookDeliveryClient webhookClient() { return webhookClient.get(); }
}
```

---

## Patrón receptor de webhooks (endpoint HTTP)

```java
public class WebhookReceiverResource implements HttpResource {

    private final WebhookVerifier verifier;
    private final EventProcessor  processor;

    @Override
    public boolean post(HttpExchange x) {
        WebhookPayload payload  = WebhookPayload.from(x);
        WebhookSignature sig    = WebhookSignature.of(x.headerFirst("X-Hub-Signature-256"));
        WebhookVerificationResult r = verifier.verify(payload, sig);

        if (!r.isValid()) { x.noContent(401); return true; }

        processor.process(payload.eventType(), payload.body());
        x.noContent(200);
        return true;
    }

    @Override
    public Set<String> supportedMethods() { return Set.of("POST"); }
}
```

---

## Más información

- [Guía ether-http-client](ether-http-client.md) — `EtherHttpClient` usado en la entrega
- [Guía ether-http-core](ether-http-core.md) — `HttpResource` para el receptor
- [Javadoc API](../api/doxygen/html/index.html)
