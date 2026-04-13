# Guía práctica: ether-ai-core

**ether-ai-core** define los contratos neutrales para capacidades de GenAI en el ecosistema Ether.
El código de dominio depende solo de estas interfaces; los adapters (`ether-ai-openai`, `ether-ai-deepseek`)
los implementan.

## Instalación

```xml
<dependency>
    <groupId>dev.rafex.ether.ai</groupId>
    <artifactId>ether-ai-core</artifactId>
    <version>8.0.0-SNAPSHOT</version>
</dependency>
```

## Contratos

| Tipo | Rol |
|---|---|
| `AiChatModel` | Interfaz principal: `chat(AiChatRequest) → AiChatResponse` |
| `AiMessage` | Mensaje con rol y contenido; métodos de fábrica `system()`, `user()`, `assistant()` |
| `AiMessageRole` | Enum de roles (`SYSTEM`, `USER`, `ASSISTANT`) con `wireValue()` |
| `AiChatRequest` | Lista de mensajes + modelo + maxTokens |
| `AiChatResponse` | Texto de respuesta + `AiUsage` (tokens) |
| `AiUsage` | `promptTokens`, `completionTokens`, `totalTokens` |
| `AiHttpException` | Error HTTP: `statusCode()` + `responseBody()` |

---

## AiMessage

```java
AiMessage sys  = AiMessage.system("Eres un asistente útil.");
AiMessage user = AiMessage.user("¿Qué es la JVM?");
AiMessage asst = AiMessage.assistant("La JVM es la máquina virtual de Java.");
```

---

## AiChatRequest

```java
// Un solo mensaje de usuario
AiChatRequest req = AiChatRequest.of("¿Qué es Java?");

// Historial completo + parámetros
AiChatRequest req = new AiChatRequest(
    List.of(
        AiMessage.system("Responde en español."),
        AiMessage.user("¿Qué es el bytecode?")
    ),
    "gpt-4o-mini",  // null → usa el default del adapter
    512             // null → usa el default del adapter
);
```

---

## AiChatModel — uso provider-agnostic

```java
public class SupportAssistant {

    private final AiChatModel model;

    public SupportAssistant(AiChatModel model) {
        this.model = model;
    }

    public String answer(String question) throws AiHttpException {
        var request = new AiChatRequest(
            List.of(
                AiMessage.system("Eres un agente de soporte técnico."),
                AiMessage.user(question)
            ),
            null, 512
        );
        return model.chat(request).text();
    }
}
```

El adapter concreto se inyecta en el contenedor de dependencias:

```java
// OpenAI
AiChatModel model = new OpenAiChatModel(OpenAiConfig.of(System.getenv("OPENAI_API_KEY")));

// DeepSeek — mismo servicio, distinto provider
AiChatModel model = new DeepSeekChatModel(DeepSeekConfig.of(System.getenv("DEEPSEEK_API_KEY")));

var assistant = new SupportAssistant(model);
```

---

## AiHttpException

```java
try {
    AiChatResponse r = model.chat(request);
} catch (AiHttpException e) {
    System.err.println("HTTP " + e.statusCode() + ": " + e.responseBody());
}
```

---

## Más información

- [Guía ether-ai-openai](ether-ai-openai.md) — adapter para GPT-4o y Azure OpenAI
- [Guía ether-ai-deepseek](ether-ai-deepseek.md) — adapter para DeepSeek-Chat y DeepSeek-Reasoner
- [Javadoc API](../api/doxygen/html/index.html)
