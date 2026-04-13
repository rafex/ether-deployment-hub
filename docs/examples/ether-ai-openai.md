# Guía práctica: ether-ai-openai

**ether-ai-openai** es el adapter de Ether para la API de OpenAI (y Azure OpenAI), implementado
con `java.net.http.HttpClient` y `ether-json`. No usa el SDK oficial de OpenAI.

## Instalación

```xml
<dependency>
    <groupId>dev.rafex.ether.ai</groupId>
    <artifactId>ether-ai-openai</artifactId>
    <version>8.0.0-SNAPSHOT</version>
</dependency>
```

---

## OpenAiConfig

```java
// Rápido — modelo y URI por defecto
OpenAiConfig config = OpenAiConfig.of(System.getenv("OPENAI_API_KEY"));

// Completo
OpenAiConfig config = new OpenAiConfig(
    System.getenv("OPENAI_API_KEY"),
    "gpt-4o-mini",
    "https://api.openai.com/v1/chat/completions"
);
```

### Azure OpenAI

```java
OpenAiConfig azure = new OpenAiConfig(
    System.getenv("AZURE_OPENAI_KEY"),
    "gpt-4o",
    "https://<resource>.openai.azure.com/openai/deployments/<deployment>/chat/completions?api-version=2024-02-01"
);
```

---

## Uso básico

```java
OpenAiChatModel model    = new OpenAiChatModel(config);
AiChatResponse  response = model.chat(AiChatRequest.of("¿Qué es el garbage collector?"));

System.out.println(response.text());
System.out.println("Tokens: " + response.usage().totalTokens());
```

---

## Conversación multi-turno

```java
List<AiMessage> history = new ArrayList<>();
history.add(AiMessage.system("Eres un tutor de Java."));

history.add(AiMessage.user("¿Qué es un record?"));
AiChatResponse r1 = model.chat(new AiChatRequest(history, null, null));
history.add(AiMessage.assistant(r1.text()));

history.add(AiMessage.user("¿Puedo extender un record?"));
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
        case 429 -> System.err.println("Rate limit");
        case 500 -> System.err.println("Error OpenAI: " + e.responseBody());
        default  -> System.err.println("HTTP " + e.statusCode());
    }
}
```

---

## Inyección con ether-di

```java
public class AiContainer {
    private final Lazy<OpenAiConfig>    config = new Lazy<>(() ->
            OpenAiConfig.of(System.getenv("OPENAI_API_KEY")));
    private final Lazy<OpenAiChatModel> model  = new Lazy<>(() ->
            new OpenAiChatModel(config.get()));

    public AiChatModel chatModel() { return model.get(); }
}
```

---

## Más información

- [Guía ether-ai-core](ether-ai-core.md) — contratos compartidos
- [Guía ether-ai-deepseek](ether-ai-deepseek.md) — adapter alternativo
- [Javadoc API](../api/doxygen/html/index.html)
