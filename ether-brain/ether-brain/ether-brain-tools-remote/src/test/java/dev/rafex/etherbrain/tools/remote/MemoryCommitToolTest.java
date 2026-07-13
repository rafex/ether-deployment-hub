package dev.rafex.etherbrain.tools.remote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rafex.etherbrain.ports.auth.TokenProvider;
import org.junit.jupiter.api.Test;

class MemoryCommitToolTest {

    private static final TokenProvider STATIC_TOKEN = () -> "test-api-key";

    private static MemoryCommitTool tool() {
        var memoryProvider = new FaissMemoryProvider("https://127.0.0.1:1/", "docs", STATIC_TOKEN, true, 60);
        return new MemoryCommitTool("https://127.0.0.1:1/", "docs", STATIC_TOKEN, memoryProvider, true);
    }

    // ── name / description / schema ────────────────────────────────────────────

    @Test
    void nameIsMemoryCommit() {
        assertEquals("memory_commit", tool().name());
    }

    @Test
    void descriptionIsNotBlank() {
        assertFalse(tool().description().isBlank());
    }

    @Test
    void inputSchemaRequiresSummary() {
        assertTrue(tool().inputSchema().contains("\"required\": [\"summary\"]"));
    }

    // ── execute() validation (no network) ─────────────────────────────────────

    @Test
    void executeBlankSummaryReturnsError() throws Exception {
        // label is provided so context.sessionId() is not dereferenced before
        // the blank-summary guard returns.
        var result = tool().execute("{\"summary\":\"  \",\"label\":\"x\"}", null);
        assertFalse(result.success());
        assertTrue(result.content().contains("summary must not be blank"));
    }
}
