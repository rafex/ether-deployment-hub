# Guía práctica: ether-brain

**ether-brain** es el runtime de agentes IA del ecosistema Ether, construido sobre
`ether-ai-core` con arquitectura hexagonal. Orquesta sesiones, prompts, llamadas al modelo
y ejecución de tools sin depender de frameworks pesados.

## Estado

> `ether-brain` está en desarrollo activo. La API pública puede cambiar entre versiones.

## Maven Dependency

```xml
<dependency>
    <groupId>dev.rafex.ether.brain</groupId>
    <artifactId>ether-brain-core</artifactId>
    <version>0.1.1</version>
</dependency>
```

---

## Principios de diseño

- **Contratos explícitos**: el runtime se entiende leyendo interfaces pequeñas, no anotaciones
- **Arquitectura hexagonal**: puertos y adaptadores sobre acoplamiento a proveedores
- **Loop confiable primero**: un agente de un solo turno antes de multiagente
- **Observable desde el inicio**: cada llamada al modelo y tool execution es trazable

---

## Conceptos clave

### Session
Una sesión encapsula el historial de mensajes de una conversación con el agente:

```java
AgentSession session = AgentSession.create("user-123");
session.addMessage(AiMessage.user("¿Cuánto es 2+2?"));
```

### Tool
Un tool es una función que el agente puede invocar durante su razonamiento:

```java
Tool calculadora = Tool.of(
    "calculadora",
    "Evalúa expresiones matemáticas simples",
    params -> {
        String expr = params.get("expresion");
        // evaluar...
        return ToolResult.success(resultado);
    }
);
```

### AgentRunner
El loop principal del agente: model call → tool execution → model call:

```java
AiChatModel model = new OpenAiChatModel(OpenAiConfig.of(System.getenv("OPENAI_API_KEY")));

AgentRunner runner = AgentRunner.builder()
    .model(model)
    .systemPrompt("Eres un asistente de análisis de datos.")
    .tools(calculadora, buscador)
    .maxTurns(10)
    .build();

AgentResult result = runner.run(session);
String response = result.lastMessage().content();
```

---

## Estructura de módulos

```
ether-brain/
├── ether-brain-ports/          ← Contratos: AgentSession, Tool, AgentRunner, etc.
├── ether-brain-core/           ← Implementación del loop de agente
├── ether-brain-infra-*/        ← Adaptadores (memoria, persistencia)
├── ether-brain-bootstrap/      ← DI con ether-di
└── ether-brain-architecture-tests/ ← Reglas ArchUnit
```

---

## Integración con ether-di

```java
public class BrainContainer {

    private final Lazy<AiChatModel> model = new Lazy<>(() ->
            new OpenAiChatModel(OpenAiConfig.of(System.getenv("OPENAI_API_KEY"))));

    private final Lazy<AgentRunner> runner = new Lazy<>(() ->
            AgentRunner.builder()
                .model(model.get())
                .systemPrompt("Eres un asistente especializado en Java.")
                .maxTurns(5)
                .build());

    public AgentRunner agentRunner() { return runner.get(); }
}
```

---

## Más información

- [Guía ether-ai-core](ether-ai-core.md) — contratos `AiChatModel` usados por ether-brain
- [Guía ether-ai-openai](ether-ai-openai.md) — adapter OpenAI
- [Guía ether-ai-deepseek](ether-ai-deepseek.md) — adapter DeepSeek
- [Código fuente](https://github.com/rafex/ether-brain) — incluyendo `agents/SPEC.md` y `agents/ARCHITECTURE.md`
- [Javadoc API](../api/doxygen/html/index.html)
