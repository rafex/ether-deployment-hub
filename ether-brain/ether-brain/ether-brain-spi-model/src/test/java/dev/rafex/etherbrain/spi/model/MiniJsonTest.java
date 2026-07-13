package dev.rafex.etherbrain.spi.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MiniJsonTest {

    // ── toJson ─────────────────────────────────────────────────────────────────

    @Test
    void toJsonSerializesFlatMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("model", "gpt-4o");
        map.put("stream", true);
        map.put("n", 1);
        assertEquals("{\"model\":\"gpt-4o\",\"stream\":true,\"n\":1}", MiniJson.toJson(map));
    }

    @Test
    void toJsonSerializesNestedMapAndList() {
        var inner = new LinkedHashMap<String, Object>();
        inner.put("role", "user");
        var map = new LinkedHashMap<String, Object>();
        map.put("messages", List.of(inner));
        assertEquals("{\"messages\":[{\"role\":\"user\"}]}", MiniJson.toJson(map));
    }

    @Test
    void toJsonHandlesNullValue() {
        var map = new LinkedHashMap<String, Object>();
        map.put("stop", null);
        assertEquals("{\"stop\":null}", MiniJson.toJson(map));
    }

    // ── appendValue escaping ────────────────────────────────────────────────────

    @Test
    void appendValueEscapesSpecialChars() {
        var sb = new StringBuilder();
        MiniJson.appendValue(sb, "line1\nquote\"back\\slash");
        assertEquals("\"line1\\nquote\\\"back\\\\slash\"", sb.toString());
    }

    @Test
    void appendValueWritesNumbersAndBooleans() {
        var sb = new StringBuilder();
        MiniJson.appendValue(sb, 42);
        MiniJson.appendValue(sb, false);
        assertEquals("42false", sb.toString());
    }

    // ── extractString ──────────────────────────────────────────────────────────

    @Test
    void extractStringReadsSimpleField() {
        String json = "{\"role\":\"assistant\",\"content\":\"hello world\"}";
        assertEquals("hello world", MiniJson.extractString(json, "content"));
    }

    @Test
    void extractStringUnescapesSequences() {
        String json = "{\"content\":\"a\\nb\\tc\\\"d\\\\e\"}";
        assertEquals("a\nb\tc\"d\\e", MiniJson.extractString(json, "content"));
    }

    @Test
    void extractStringReturnsNullWhenKeyMissing() {
        assertNull(MiniJson.extractString("{\"other\":\"x\"}", "content"));
    }

    @Test
    void extractStringReturnsNullWhenNoStringValue() {
        assertNull(MiniJson.extractString("{\"content\":", "content"));
    }

    @Test
    void extractStringHandlesEmptyValue() {
        assertEquals("", MiniJson.extractString("{\"content\":\"\"}", "content"));
    }

    @Test
    void roundTripThroughToJsonAndExtract() {
        var map = Map.<String, Object>of("content", "value-42");
        String json = MiniJson.toJson(map);
        assertEquals("value-42", MiniJson.extractString(json, "content"));
    }
}
