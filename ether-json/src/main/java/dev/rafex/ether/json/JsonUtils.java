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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;

public final class JsonUtils {

    private static final JacksonJsonCodec DEFAULT_CODEC = JacksonJsonCodec.defaultCodec();

    private JsonUtils() {
    }

    public static String toJson(final Object value) {
        return DEFAULT_CODEC.toJson(value);
    }

    public static String toPrettyJson(final Object value) {
        return DEFAULT_CODEC.toPrettyJson(value);
    }

    public static byte[] toJsonBytes(final Object value) {
        return DEFAULT_CODEC.toJsonBytes(value);
    }

    public static void writeJson(final OutputStream output, final Object value) {
        DEFAULT_CODEC.writeValue(output, value);
    }

    public static <T> T fromJson(final String json, final Class<T> clazz) {
        return DEFAULT_CODEC.readValue(json, clazz);
    }

    public static <T> T fromJson(final String json, final TypeReference<T> typeRef) {
        return DEFAULT_CODEC.readValue(json, typeRef);
    }

    public static <T> T fromJson(final String json, final JavaType type) {
        return DEFAULT_CODEC.readValue(json, type);
    }

    public static <T> T fromJson(final InputStream input, final Class<T> clazz) {
        return DEFAULT_CODEC.readValue(input, clazz);
    }

    public static <T> T fromJson(final InputStream input, final TypeReference<T> typeRef) {
        return DEFAULT_CODEC.readValue(input, typeRef);
    }

    public static <T> T fromJson(final InputStream input, final JavaType type) {
        return DEFAULT_CODEC.readValue(input, type);
    }

    public static <T> T fromJson(final byte[] input, final Class<T> clazz) {
        return DEFAULT_CODEC.readValue(input, clazz);
    }

    public static <T> T fromJson(final byte[] input, final TypeReference<T> typeRef) {
        return DEFAULT_CODEC.readValue(input, typeRef);
    }

    public static <T> T fromJson(final byte[] input, final JavaType type) {
        return DEFAULT_CODEC.readValue(input, type);
    }

    public static <T> List<T> fromJsonToList(final String json, final Class<T> elementClass) {
        final var listType = DEFAULT_CODEC.mapper().getTypeFactory().constructCollectionType(List.class, elementClass);
        return DEFAULT_CODEC.readValue(json, listType);
    }

    public static JsonNode parseTree(final String json) {
        return DEFAULT_CODEC.readTree(json);
    }

    public static JsonNode parseTree(final InputStream input) {
        return DEFAULT_CODEC.readTree(input);
    }

    public static JsonNode parseTree(final byte[] input) {
        return DEFAULT_CODEC.readTree(input);
    }

    public static JsonNode readTreeFromFile(final Path path) {
        try (InputStream input = Files.newInputStream(path)) {
            return DEFAULT_CODEC.readTree(input);
        } catch (final IOException e) {
            throw new JsonCodecException("Error leyendo JSON desde fichero", e);
        }
    }

    public static JsonNode valueToTree(final Object value) {
        return DEFAULT_CODEC.valueToTree(value);
    }

    public static JsonNode at(final JsonNode node, final String pointer) {
        return DEFAULT_CODEC.at(node, pointer);
    }

    public static <T> T treeToValue(final JsonNode node, final Class<T> clazz) {
        return DEFAULT_CODEC.treeToValue(node, clazz);
    }

    public static <T> T treeToValue(final JsonNode node, final TypeReference<T> typeRef) {
        return DEFAULT_CODEC.treeToValue(node, typeRef);
    }

    public static JsonCodec codec() {
        return DEFAULT_CODEC;
    }

    public static JsonCodec codec(final JsonCodecBuilder builder) {
        return builder.build();
    }
}
