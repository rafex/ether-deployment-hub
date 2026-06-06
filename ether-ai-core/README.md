# ether-ai-core

Contratos neutrales para capacidades de GenAI dentro del ecosistema Ether.
Este módulo concentra tipos y APIs compartidas sin acoplarse a ningún proveedor concreto.
Los adapters (`ether-ai-openai`, `ether-ai-deepseek`) implementan estas interfaces.

## Maven Dependency

```xml
<dependency>
    <groupId>dev.rafex.ether.ai</groupId>
    <artifactId>ether-ai-core</artifactId>
    <version>8.0.0-SNAPSHOT</version>
</dependency>
```

---

## Contratos principales

### `AiChatModel` — interfaz del modelo de chat

```java
public interface AiChatModel {
    AiChatResponse chat(AiChatRequest request) throws AiHttpException;
}
```

Implementa esta interfaz para añadir soporte a cualquier proveedor.
El código de dominio solo depende de `AiChatModel`, nunca del adapter concreto.

---

### `AiMessage` — un mensaje en la conversación

```java
// Métodos de fábrica
AiMessage sys  = AiMessage.system("Eres un asistente útil.");
AiMessage user = AiMessage.user("¿Cuánto es 2 + 2?");
AiMessage asst = AiMessage.assistant("4");

// Constructor completo
AiMessage msg = new AiMessage(AiMessageRole.USER, "Hola");
String role    = msg.role().wireValue(); // "user"
String content = msg.content();         // "Hola"
```

---

### `AiMessageRole` — roles reconocidos por la API

| Constante | `wireValue()` | Uso |
|---|---|---|
| `SYSTEM` | `"system"` | Instrucción de sistema inicial |
| `USER` | `"user"` | Turno del usuario |
| `ASSISTANT` | `"assistant"` | Turno del modelo |

---

### `AiChatRequest` — solicitud al modelo

```java
// Constructor rápido: un solo mensaje de usuario
AiChatRequest req = AiChatRequest.of("¿Qué es Java?");

// Constructor completo: historial de conversación + parámetros
AiChatRequest req = new AiChatRequest(
    List.of(
        AiMessage.system("Responde en español."),
        AiMessage.user("¿Qué es una JVM?")
    ),
    "gpt-4o-mini",   // modelo (puede ser null → usa default del adapter)
    1024             // maxTokens (puede ser null → usa default del adapter)
);

List<AiMessage> msgs = req.messages();
String model         = req.model();
Integer maxTokens    = req.maxTokens();
```

---

### `AiChatResponse` — respuesta del modelo

```java
AiChatResponse response = model.chat(request);

String text    = response.text();    // contenido del mensaje del asistente
AiUsage usage  = response.usage();   // tokens consumidos
```

---

### `AiUsage` — tokens consumidos

```java
AiUsage usage = response.usage();

int promptTokens     = usage.promptTokens();
int completionTokens = usage.completionTokens();
int totalTokens      = usage.totalTokens();
```

---

### `AiHttpException` — error HTTP del proveedor

```java
try {
    AiChatResponse resp = model.chat(request);
} catch (AiHttpException e) {
    int    status = e.statusCode();    // 401, 429, 500, …
    String body   = e.responseBody();  // cuerpo raw de la respuesta
    // manejar según status
}
```

---

## Patrón de uso provider-agnostic

```java
// Servicio de dominio — solo depende de ether-ai-core
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
            null,  // modelo por defecto del adapter
            512
        );
        return model.chat(request).text();
    }
}

// Wiring en el contenedor — inyecta el adapter concreto
var model     = new OpenAiChatModel(OpenAiConfig.of(System.getenv("OPENAI_API_KEY")));
var assistant = new SupportAssistant(model);

// Para cambiar a DeepSeek solo se cambia esta línea:
// var model = new DeepSeekChatModel(DeepSeekConfig.of(System.getenv("DEEPSEEK_API_KEY")));
```

---

## Compatibilidad

- Java 21+
- Sin dependencias externas (solo `ether-json` en los adapters)
- Compatible con GraalVM native-image (sin reflexión)

---

## Módulos relacionados

| Módulo | Descripción |
|---|---|
| [ether-ai-openai](../ether-ai-openai/README.md) | Adapter para GPT-4o, GPT-4o-mini y modelos compatibles con Azure OpenAI |
| [ether-ai-deepseek](../ether-ai-deepseek/README.md) | Adapter para DeepSeek-Chat y DeepSeek-Reasoner |
| [ether-brain](../ether-brain/README.md) | Agente de IA de alto nivel sobre ether-ai-core |
