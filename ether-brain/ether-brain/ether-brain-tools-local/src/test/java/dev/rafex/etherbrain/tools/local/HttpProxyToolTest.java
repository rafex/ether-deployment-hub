package dev.rafex.etherbrain.tools.local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HttpProxyToolTest {

    private static final String SCHEMA = "{\"type\":\"object\"}";

    // ── resolveEnv() ───────────────────────────────────────────────────────────

    @Test
    void resolveEnvNullReturnsNull() {
        assertNull(HttpProxyTool.resolveEnv(null));
    }

    @Test
    void resolveEnvNoPlaceholderReturnsSame() {
        assertEquals("plain-value", HttpProxyTool.resolveEnv("plain-value"));
    }

    @Test
    void resolveEnvSubstitutesFromSystemProperty() {
        System.setProperty("HTTP_PROXY_TOOL_TEST_TOKEN", "secret123");
        try {
            assertEquals("Bearer secret123",
                    HttpProxyTool.resolveEnv("Bearer ${HTTP_PROXY_TOOL_TEST_TOKEN}"));
        } finally {
            System.clearProperty("HTTP_PROXY_TOOL_TEST_TOKEN");
        }
    }

    @Test
    void resolveEnvUnknownVarReturnsEmptySubstitution() {
        assertEquals("prefix-", HttpProxyTool.resolveEnv("prefix-${DOES_NOT_EXIST_ANYWHERE_XYZ}"));
    }

    // ── accessors ──────────────────────────────────────────────────────────────

    @Test
    void exposesNameDescriptionAndSchema() {
        var tool = new HttpProxyTool("search", "Searches", SCHEMA,
                "http://localhost:1/x", "POST", Map.of(), 15);
        assertEquals("search", tool.name());
        assertEquals("Searches", tool.description());
        assertEquals(SCHEMA, tool.inputSchema());
    }

    // ── execute() against embedded HTTP server ─────────────────────────────────

    @Test
    void executePostReturnsBodyOnSuccess() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/echo", exchange -> {
            byte[] body = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        try {
            String endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/echo";
            var tool = new HttpProxyTool("echo", "d", SCHEMA, endpoint, "POST", Map.of(), 5);
            var result = tool.execute("{\"q\":\"hi\"}", null);
            assertTrue(result.success());
            assertEquals("{\"ok\":true}", result.content());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void executeReturnsFailureOnNon2xx() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/boom", exchange -> {
            byte[] body = "server error".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, body.length);
            try (var os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        try {
            String endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/boom";
            var tool = new HttpProxyTool("boom", "d", SCHEMA, endpoint, "POST", Map.of(), 5);
            var result = tool.execute("{}", null);
            assertFalse(result.success());
            assertTrue(result.content().contains("HTTP 500"));
        } finally {
            server.stop(0);
        }
    }
}
