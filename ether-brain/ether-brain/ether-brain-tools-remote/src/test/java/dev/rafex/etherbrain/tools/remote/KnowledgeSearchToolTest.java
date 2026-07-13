package dev.rafex.etherbrain.tools.remote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rafex.etherbrain.ports.auth.TokenProvider;
import org.junit.jupiter.api.Test;

class KnowledgeSearchToolTest {

    private static final TokenProvider STATIC_TOKEN = () -> "test-api-key";

    private static KnowledgeSearchTool toolWithDefaultNamespace() {
        return new KnowledgeSearchTool(STATIC_TOKEN, true, "docs");
    }

    private static KnowledgeSearchTool toolWithoutDefaultNamespace() {
        return new KnowledgeSearchTool(STATIC_TOKEN, true, null);
    }

    // ── name / description / schema ────────────────────────────────────────────

    @Test
    void nameIsKnowledgeSearch() {
        assertEquals("knowledge_search", toolWithoutDefaultNamespace().name());
    }

    @Test
    void descriptionIncludesDefaultNamespace() {
        assertTrue(toolWithDefaultNamespace().description().contains("docs"));
    }

    @Test
    void descriptionWithoutDefaultNamespaceOmitsIt() {
        assertFalse(toolWithoutDefaultNamespace().description().contains("Default namespace"));
    }

    @Test
    void inputSchemaAlwaysRequiresQuery() {
        assertTrue(toolWithDefaultNamespace().inputSchema().contains("\"query\""));
    }

    @Test
    void inputSchemaRequiresNamespaceWhenNoDefault() {
        String schema = toolWithoutDefaultNamespace().inputSchema();
        assertTrue(schema.contains("\"query\", \"namespace\""));
    }

    @Test
    void inputSchemaDoesNotRequireNamespaceWhenDefaultPresent() {
        String schema = toolWithDefaultNamespace().inputSchema();
        assertFalse(schema.contains("\"query\", \"namespace\""));
    }

    // ── execute() validation (no network) ─────────────────────────────────────

    @Test
    void executeBlankQueryReturnsError() throws Exception {
        var result = toolWithDefaultNamespace().execute("{\"query\":\"  \"}", null);
        assertFalse(result.success());
        assertTrue(result.content().contains("query must not be blank"));
    }

    @Test
    void executeMissingNamespaceReturnsError() throws Exception {
        var result = toolWithoutDefaultNamespace().execute("{\"query\":\"hola\"}", null);
        assertFalse(result.success());
        assertTrue(result.content().contains("namespace is required"));
    }
}
