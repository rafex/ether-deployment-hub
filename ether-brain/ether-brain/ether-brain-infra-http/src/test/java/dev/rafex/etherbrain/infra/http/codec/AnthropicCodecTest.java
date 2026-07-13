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

class AnthropicCodecTest {

    private static HttpModelConfig config(String baseUrl, String model) {
        return new HttpModelConfig(URI.create(baseUrl), "test-key", model,
                4096, java.time.Duration.ofSeconds(30));
    }

    // ── endpoint() ────────────────────────────────────────────────────────────

    @Test
    void endpointFromBaseUrl() {
        String url = AnthropicCodec.endpoint(config("https://api.anthropic.com", "claude-3-5-sonnet"));
        assertEquals("https://api.anthropic.com/v1/messages", url);
    }

    @Test
    void endpointAlreadyFull() {
        String full = "https://api.anthropic.com/v1/messages";
        String url = AnthropicCodec.endpoint(config(full, "claude-3-5-sonnet"));
        assertEquals(full, url);
    }

    @Test
    void endpointWithV1Suffix() {
        String url = AnthropicCodec.endpoint(config("https://proxy.example.com/v1", "claude-3-5-sonnet"));
        assertEquals("https://proxy.example.com/v1/messages", url);
    }

    // ── parseResponse() — texto ───────────────────────────────────────────────

    @Test
    void parsesFinalAnswer() {
        String body = """
                {
                  "content": [{"type": "text", "text": "Hola desde Claude."}],
                  "stop_reason": "end_turn"
                }
                """;
        var response = new AnthropicCodec().parseResponse(body);
        assertInstanceOf(FinalAnswer.class, response);
        assertEquals("Hola desde Claude.", ((FinalAnswer) response).content());
    }

    @Test
    void parsesMultipleTextBlocks() {
        String body = """
                {
                  "content": [
                    {"type": "text", "text": "Parte 1. "},
                    {"type": "text", "text": "Parte 2."}
                  ],
                  "stop_reason": "end_turn"
                }
                """;
        var response = new AnthropicCodec().parseResponse(body);
        assertInstanceOf(FinalAnswer.class, response);
        assertEquals("Parte 1. Parte 2.", ((FinalAnswer) response).content());
    }

    @Test
    void returnsEmptyWhenNoTextBlocks() {
        String body = """
                {
                  "content": [],
                  "stop_reason": "end_turn"
                }
                """;
        var response = new AnthropicCodec().parseResponse(body);
        assertInstanceOf(FinalAnswer.class, response);
        assertEquals("", ((FinalAnswer) response).content());
    }

    // ── parseResponse() — tool call ───────────────────────────────────────────

    @Test
    void parsesToolCall() {
        String body = """
                {
                  "content": [
                    {"type": "text", "text": "Voy a consultar la hora."},
                    {"type": "tool_use", "id": "tu-abc123", "name": "current_time", "input": {}}
                  ],
                  "stop_reason": "tool_use"
                }
                """;
        var response = new AnthropicCodec().parseResponse(body);
        assertInstanceOf(ToolRequest.class, response);
        var tr = (ToolRequest) response;
        assertEquals("current_time", tr.toolName());
        assertEquals("tu-abc123", tr.toolCallId());
        assertEquals("{}", tr.arguments());
    }

    @Test
    void parsesMultipleToolCalls() {
        String body = """
                {
                  "content": [
                    {"type": "tool_use", "id": "t1", "name": "a", "input": {}},
                    {"type": "tool_use", "id": "t2", "name": "b", "input": {"x": 1}}
                  ],
                  "stop_reason": "tool_use"
                }
                """;
        var response = new AnthropicCodec().parseResponse(body);
        assertInstanceOf(BatchedToolRequest.class, response);
        assertEquals(2, ((BatchedToolRequest) response).size());
    }

    // ── parseResponse() — error ───────────────────────────────────────────────

    @Test
    void throwsOnApiError() {
        String body = """
                {
                  "type": "error",
                  "error": {"type": "authentication_error", "message": "invalid x-api-key"}
                }
                """;
        var ex = assertThrows(RuntimeException.class, () -> new AnthropicCodec().parseResponse(body));
        assertTrue(ex.getMessage().contains("authentication_error"));
        assertTrue(ex.getMessage().contains("invalid x-api-key"));
    }

    @Test
    void throwsWhenToolUseButNoBlock() {
        String body = """
                {
                  "content": [{"type": "text", "text": "sin bloque tool_use"}],
                  "stop_reason": "tool_use"
                }
                """;
        var ex = assertThrows(RuntimeException.class, () -> new AnthropicCodec().parseResponse(body));
        assertTrue(ex.getMessage().contains("no tool_use block"));
    }

    // ── supportsStreaming() ───────────────────────────────────────────────────

    @Test
    void supportsStreaming() {
        assertTrue(new AnthropicCodec().supportsStreaming());
    }
}
