/*-
 * #%L
 * ether-json
 * %%
 * Copyright (C) 2025 Raúl Eduardo González Argote
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

package dev.rafex.ether.json;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

class JsonUtilsTest {

    record User(String name, int age, LocalDate bornAt) {
    }

    @Test
    void shouldSerializeDeserializeRoundTrip() {
        final var input = new User("Rafex", 30, LocalDate.parse("1995-03-07"));

        final var json = JsonUtils.toJson(input);
        final var out = JsonUtils.fromJson(json, User.class);

        assertEquals(input, out);
    }

    @Test
    void shouldSupportGenericDeserialization() {
        final String json = "{\"values\":[1,2,3]}";

        final Map<String, List<Integer>> out = JsonUtils.fromJson(json,
                new TypeReference<Map<String, List<Integer>>>() {
                });

        assertEquals(List.of(1, 2, 3), out.get("values"));
    }

    @Test
    void shouldReadAndWriteBytesAndStreams() {
        final var map = Map.of("ok", true, "n", 7);

        final byte[] bytes = JsonUtils.toJsonBytes(map);
        final var fromBytes = JsonUtils.fromJson(bytes, new TypeReference<Map<String, Object>>() {
        });
        assertEquals(Boolean.TRUE, fromBytes.get("ok"));

        final var in = new ByteArrayInputStream(bytes);
        final var fromStream = JsonUtils.fromJson(in, new TypeReference<Map<String, Object>>() {
        });
        assertEquals(7, ((Number) fromStream.get("n")).intValue());

        final var out = new ByteArrayOutputStream();
        JsonUtils.writeJson(out, map);
        assertArrayEquals(bytes, out.toByteArray());
    }

    @Test
    void shouldExposeTreeUtilities() {
        final var tree = JsonUtils.parseTree("{\"a\":{\"b\":42}}");
        final var target = JsonUtils.at(tree, "/a/b");

        assertNotNull(target);
        assertEquals(42, target.asInt());
    }

    @Test
    void shouldThrowTypedExceptionOnInvalidJson() {
        final var ex = assertThrows(JsonCodecException.class, () -> JsonUtils.fromJson("{", User.class));
        assertTrue(ex.getMessage().toLowerCase().contains("error"));
    }

    @Test
    void shouldRenderPrettyJson() {
        final var json = JsonUtils.toPrettyJson(Map.of("name", "ether"));
        assertTrue(json.contains("\n"));
        assertTrue(json.contains("\"name\""));
    }

    @Test
    void shouldParseFromUtf8Bytes() {
        final byte[] bytes = "{\"lang\":\"es\"}".getBytes(StandardCharsets.UTF_8);
        final var node = JsonUtils.parseTree(bytes);
        assertEquals("es", node.get("lang").asText());
    }
}
