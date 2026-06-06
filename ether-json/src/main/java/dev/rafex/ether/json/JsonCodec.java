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

import java.io.InputStream;
import java.io.OutputStream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;

public interface JsonCodec {

    String toJson(Object value);

    String toPrettyJson(Object value);

    byte[] toJsonBytes(Object value);

    void writeValue(OutputStream output, Object value);

    <T> T readValue(InputStream input, Class<T> type);

    <T> T readValue(InputStream input, TypeReference<T> typeRef);

    <T> T readValue(InputStream input, JavaType type);

    <T> T readValue(String content, Class<T> type);

    <T> T readValue(String content, TypeReference<T> typeRef);

    <T> T readValue(String content, JavaType type);

    <T> T readValue(byte[] content, Class<T> type);

    <T> T readValue(byte[] content, TypeReference<T> typeRef);

    <T> T readValue(byte[] content, JavaType type);

    JsonNode readTree(String content);

    JsonNode readTree(InputStream input);

    JsonNode readTree(byte[] input);

    JsonNode valueToTree(Object value);

    <T> T treeToValue(JsonNode node, Class<T> type);

    <T> T treeToValue(JsonNode node, TypeReference<T> typeRef);

    JsonNode at(JsonNode node, String pointer);
}
