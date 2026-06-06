# ether-ai-deepseek

Adapter de Ether para DeepSeek implementado con `java.net.http.HttpClient` y `ether-json`,
consumiendo la API HTTP directamente y mapeando las respuestas a los contratos de `ether-ai-core`.
No depende de ningún SDK oficial de DeepSeek.

## Maven Dependency

```xml
<dependency>
    <groupId>dev.rafex.ether.ai</groupId>
    <artifactId>ether-ai-deepseek</artifactId>
    <version>8.0.0-SNAPSHOT</version>
</dependency>
```

---

## Configuración: `DeepSeekConfig`

```java
// Constructor rápido — modelo y URI por defecto
DeepSeekConfig config = DeepSeekConfig.of(System.getenv("DEEPSEEK_API_KEY"));

// Constructor completo
DeepSeekConfig config = new DeepSeekConfig(
    System.getenv("DEEPSEEK_API_KEY"),                          // apiKey
    "deepseek-chat",                                            // defaultModel
    "https://api.deepseek.com/chat/completions"                 // chatCompletionsUri
);
```

---

## Uso: `DeepSeekChatModel`

```java
DeepSeekConfig    config = DeepSeekConfig.of(System.getenv("DEEPSEEK_API_KEY"));
DeepSeekChatModel model  = new DeepSeekChatModel(config);

AiChatRequest  request  = AiChatRequest.of("¿Qué ventajas tiene Java sobre C++?");
AiChatResponse response = model.chat(request);

System.out.println(response.text());
System.out.println("Tokens usados: " + response.usage().totalTokens());
```

### HttpClient personalizado

```java
HttpClient client = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(15))
    .build();

DeepSeekChatModel model = new DeepSeekChatModel(config, client);
```

---

## Conversación multi-turno

```java
List<AiMessage> history = new ArrayList<>();
history.add(AiMessage.system("Eres un experto en arquitectura de software."));

// Turno 1
history.add(AiMessage.user("¿Cuándo usar microservicios?"));
AiChatResponse r1 = model.chat(new AiChatRequest(history, null, null));
history.add(AiMessage.assistant(r1.text()));

// Turno 2
history.add(AiMessage.user("¿Y cuándo es mejor un monolito?"));
AiChatResponse r2 = model.chat(new AiChatRequest(history, null, null));
System.out.println(r2.text());
```

---

## Diferencias con OpenAI

| Aspecto | OpenAI | DeepSeek |
|---|---|---|
| Campo de tokens | `max_tokens` | `max_tokens` |
| Modelos principales | `gpt-4o`, `gpt-4o-mini` | `deepseek-chat`, `deepseek-reasoner` |
| Error de saldo insuficiente | — | HTTP 402 |
| Razonamiento avanzado | `o1`, `o3` | `deepseek-reasoner` |

---

## Manejo de errores

```java
try {
    AiChatResponse response = model.chat(request);
} catch (AiHttpException e) {
    switch (e.statusCode()) {
        case 401 -> System.err.println("API key inválida");
        case 402 -> System.err.println("Saldo insuficiente — recarga tu cuenta DeepSeek");
        case 429 -> System.err.println("Rate limit — reintenta después");
        case 500 -> System.err.println("Error interno DeepSeek: " + e.responseBody());
        default  -> System.err.println("HTTP " + e.statusCode() + ": " + e.responseBody());
    }
}
```

---

## Patrón de intercambio de proveedor

El código de dominio depende solo de `AiChatModel`. Cambiar de proveedor es una sola línea:

```java
// OpenAI
AiChatModel model = new OpenAiChatModel(OpenAiConfig.of(System.getenv("OPENAI_API_KEY")));

// DeepSeek — misma interfaz, distinto adapter
AiChatModel model = new DeepSeekChatModel(DeepSeekConfig.of(System.getenv("DEEPSEEK_API_KEY")));

// El servicio de dominio no cambia
var assistant = new SupportAssistant(model);
```

---

## Inyección con ether-di

```java
public class AiContainer {

    private final Lazy<DeepSeekConfig>    config = new Lazy<>(() ->
            DeepSeekConfig.of(System.getenv("DEEPSEEK_API_KEY")));
    private final Lazy<DeepSeekChatModel> model  = new Lazy<>(() ->
            new DeepSeekChatModel(config.get()));

    public AiChatModel chatModel() { return model.get(); }
}
```

---

## Modelos disponibles

| Modelo | Uso |
|---|---|
| `deepseek-chat` | Chat de propósito general, económico |
| `deepseek-reasoner` | Razonamiento avanzado (matemáticas, código) |

---

## Módulos relacionados

| Módulo | Descripción |
|---|---|
| [ether-ai-core](../ether-ai-core/README.md) | Contratos compartidos: `AiChatModel`, `AiMessage`, `AiChatRequest/Response` |
| [ether-ai-openai](../ether-ai-openai/README.md) | Adapter alternativo para OpenAI / Azure OpenAI |
