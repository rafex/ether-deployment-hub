# Guía práctica: ether-ai-deepseek

**ether-ai-deepseek** es el adapter de Ether para la API de DeepSeek, implementado con
`java.net.http.HttpClient` y `ether-json`. No usa ningún SDK de terceros.

## Instalación

```xml
<dependency>
    <groupId>dev.rafex.ether.ai</groupId>
    <artifactId>ether-ai-deepseek</artifactId>
    <version>8.0.0-SNAPSHOT</version>
</dependency>
```

---

## DeepSeekConfig

```java
// Rápido — modelo y URI por defecto
DeepSeekConfig config = DeepSeekConfig.of(System.getenv("DEEPSEEK_API_KEY"));

// Completo
DeepSeekConfig config = new DeepSeekConfig(
    System.getenv("DEEPSEEK_API_KEY"),
    "deepseek-chat",
    "https://api.deepseek.com/chat/completions"
);
```

---

## Uso básico

```java
DeepSeekChatModel model    = new DeepSeekChatModel(config);
AiChatResponse    response = model.chat(AiChatRequest.of("¿Cuándo usar microservicios?"));

System.out.println(response.text());
System.out.println("Tokens: " + response.usage().totalTokens());
```

---

## Conversación multi-turno

```java
List<AiMessage> history = new ArrayList<>();
history.add(AiMessage.system("Eres un experto en arquitectura de software."));

history.add(AiMessage.user("¿Cuándo usar microservicios?"));
AiChatResponse r1 = model.chat(new AiChatRequest(history, null, null));
history.add(AiMessage.assistant(r1.text()));

history.add(AiMessage.user("¿Y cuándo es mejor un monolito?"));
AiChatResponse r2 = model.chat(new AiChatRequest(history, null, null));
System.out.println(r2.text());
```

---

## Manejo de errores

```java
try {
    AiChatResponse r = model.chat(request);
} catch (AiHttpException e) {
    switch (e.statusCode()) {
        case 401 -> System.err.println("API key inválida");
        case 402 -> System.err.println("Saldo insuficiente — recarga tu cuenta DeepSeek");
        case 429 -> System.err.println("Rate limit");
        case 500 -> System.err.println("Error DeepSeek: " + e.responseBody());
        default  -> System.err.println("HTTP " + e.statusCode());
    }
}
```

---

## Intercambio de proveedor

El dominio solo depende de `AiChatModel`. Cambiar de proveedor es una línea:

```java
// OpenAI
AiChatModel model = new OpenAiChatModel(OpenAiConfig.of(System.getenv("OPENAI_API_KEY")));

// DeepSeek — mismo código de dominio
AiChatModel model = new DeepSeekChatModel(DeepSeekConfig.of(System.getenv("DEEPSEEK_API_KEY")));
```

---

## Modelos disponibles

| Modelo | Uso |
|---|---|
| `deepseek-chat` | Chat de propósito general, económico |
| `deepseek-reasoner` | Razonamiento avanzado (matemáticas, código) |

---

## Más información

- [Guía ether-ai-core](ether-ai-core.md) — contratos compartidos
- [Guía ether-ai-openai](ether-ai-openai.md) — adapter para OpenAI / Azure OpenAI
- [Javadoc API](../api/doxygen/html/index.html)
