package dev.rafex.etherbrain.infra.http.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rafex.etherbrain.infra.http.HttpModelConfig;
import dev.rafex.etherbrain.ports.model.BatchedToolRequest;
import dev.rafex.etherbrain.ports.model.FinalAnswer;
import dev.rafex.etherbrain.ports.model.ToolRequest;
import java.net.URI;
import org.junit.jupiter.api.Test;

class OpenAiCodecTest {

    private static HttpModelConfig config(String baseUrl, String model) {
        return new HttpModelConfig(URI.create(baseUrl), "test-key", model,
                4096, java.time.Duration.ofSeconds(30));
    }

    // ── endpoint() ────────────────────────────────────────────────────────────

    @Test
    void endpointFromBaseUrl() {
        String url = OpenAiCodec.endpoint(config("https://api.openai.com", "gpt-4o"));
        assertEquals("https://api.openai.com/v1/chat/completions", url);
    }

    @Test
    void endpointAlreadyFull() {
        String full = "https://api.openai.com/v1/chat/completions";
        String url = OpenAiCodec.endpoint(config(full, "gpt-4o"));
        assertEquals(full, url);
    }

    @Test
    void endpointWithV1Suffix() {
        String url = OpenAiCodec.endpoint(config("https://api.groq.com/v1", "llama-3.1-70b"));
        assertEquals("https://api.groq.com/v1/chat/completions", url);
    }

    @Test
    void endpointStripsTrailingSlash() {
        String url = OpenAiCodec.endpoint(config("https://api.openai.com/", "gpt-4o"));
        assertEquals("https://api.openai.com/v1/chat/completions", url);
    }

    // ── parseResponse() — texto ───────────────────────────────────────────────

    @Test
    void parsesFinalAnswer() {
        String body = """
                {
                  "choices": [{
                    "message": {"role": "assistant", "content": "Hola desde OpenAI."},
                    "finish_reason": "stop"
                  }]
                }
                """;
        var response = new OpenAiCodec().parseResponse(body);
        assertInstanceOf(FinalAnswer.class, response);
        assertEquals("Hola desde OpenAI.", ((FinalAnswer) response).content());
    }

    @Test
    void parsesContentArray() {
        String body = """
                {
                  "choices": [{
                    "message": {
                      "role": "assistant",
                      "content": [
                        {"type": "text", "text": "Parte 1. "},
                        {"type": "text", "text": "Parte 2."}
                      ]
                    },
                    "finish_reason": "stop"
                  }]
                }
                """;
        var response = new OpenAiCodec().parseResponse(body);
        assertInstanceOf(FinalAnswer.class, response);
        assertEquals("Parte 1. Parte 2.", ((FinalAnswer) response).content());
    }

    // ── parseResponse() — tool call ───────────────────────────────────────────

    @Test
    void parsesToolCall() {
        String body = """
                {
                  "choices": [{
                    "message": {
                      "role": "assistant",
                      "tool_calls": [{
                        "id": "call_abc123",
                        "type": "function",
                        "function": {"name": "current_time", "arguments": "{}"}
                      }]
                    },
                    "finish_reason": "tool_calls"
                  }]
                }
                """;
        var response = new OpenAiCodec().parseResponse(body);
        assertInstanceOf(ToolRequest.class, response);
        var tr = (ToolRequest) response;
        assertEquals("current_time", tr.toolName());
        assertEquals("call_abc123", tr.toolCallId());
        assertEquals("{}", tr.arguments());
    }

    @Test
    void parsesMultipleToolCalls() {
        String body = """
                {
                  "choices": [{
                    "message": {
                      "role": "assistant",
                      "tool_calls": [
                        {"id": "c1", "type": "function", "function": {"name": "a", "arguments": "{}"}},
                        {"id": "c2", "type": "function", "function": {"name": "b", "arguments": "{}"}}
                      ]
                    },
                    "finish_reason": "tool_calls"
                  }]
                }
                """;
        var response = new OpenAiCodec().parseResponse(body);
        assertInstanceOf(BatchedToolRequest.class, response);
        assertEquals(2, ((BatchedToolRequest) response).size());
    }

    @Test
    void parsesToolCallWithStopFinishReason() {
        String body = """
                {
                  "choices": [{
                    "message": {
                      "role": "assistant",
                      "tool_calls": [{
                        "id": "call_x",
                        "type": "function",
                        "function": {"name": "search", "arguments": "{}"}
                      }]
                    },
                    "finish_reason": "stop"
                  }]
                }
                """;
        var response = new OpenAiCodec().parseResponse(body);
        assertInstanceOf(ToolRequest.class, response);
        assertEquals("search", ((ToolRequest) response).toolName());
    }

    @Test
    void parsesLegacyFunctionCall() {
        String body = """
                {
                  "choices": [{
                    "message": {
                      "role": "assistant",
                      "function_call": {"name": "current_time", "arguments": "{}"}
                    },
                    "finish_reason": "function_call"
                  }]
                }
                """;
        var response = new OpenAiCodec().parseResponse(body);
        assertInstanceOf(ToolRequest.class, response);
        var tr = (ToolRequest) response;
        assertEquals("current_time", tr.toolName());
        assertEquals("fc-current_time", tr.toolCallId());
    }

    @Test
    void normalizesObjectArguments() {
        String body = """
                {
                  "choices": [{
                    "message": {
                      "role": "assistant",
                      "tool_calls": [{
                        "id": "c1",
                        "type": "function",
                        "function": {"name": "search", "arguments": {"q": "foo"}}
                      }]
                    },
                    "finish_reason": "tool_calls"
                  }]
                }
                """;
        var response = new OpenAiCodec().parseResponse(body);
        assertInstanceOf(ToolRequest.class, response);
        assertEquals("{\"q\":\"foo\"}", ((ToolRequest) response).arguments());
    }

    @Test
    void normalizesMissingArguments() {
        String body = """
                {
                  "choices": [{
                    "message": {
                      "role": "assistant",
                      "tool_calls": [{
                        "id": "c1",
                        "type": "function",
                        "function": {"name": "ping"}
                      }]
                    },
                    "finish_reason": "tool_calls"
                  }]
                }
                """;
        var response = new OpenAiCodec().parseResponse(body);
        assertInstanceOf(ToolRequest.class, response);
        assertEquals("{}", ((ToolRequest) response).arguments());
    }

    // ── parseResponse() — error ───────────────────────────────────────────────

    @Test
    void throwsOnApiError() {
        String body = """
                {
                  "error": {
                    "message": "Invalid API key.",
                    "type": "invalid_request_error"
                  }
                }
                """;
        var ex = assertThrows(RuntimeException.class, () -> new OpenAiCodec().parseResponse(body));
        assertTrue(ex.getMessage().contains("Invalid API key"));
        assertTrue(ex.getMessage().contains("invalid_request_error"));
    }

    @Test
    void throwsOnNoChoices() {
        String body = """
                {"choices": []}
                """;
        var ex = assertThrows(RuntimeException.class, () -> new OpenAiCodec().parseResponse(body));
        assertTrue(ex.getMessage().contains("No choices"));
    }

    // ── supportsStreaming() ───────────────────────────────────────────────────

    @Test
    void supportsStreaming() {
        assertTrue(new OpenAiCodec().supportsStreaming());
    }
}
