package dev.rafex.ether.http.client.model;

/*-
 * #%L
 * ether-http-client
 * %%
 * Copyright (C) 2025 - 2026 Raúl Eduardo González Argote
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

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.rafex.ether.json.JsonUtils;

public record HttpRequestSpec(HttpMethod method, URI uri, Map<String, List<String>> headers, byte[] body,
        Duration timeout) {

    public HttpRequestSpec {
        headers = normalizeHeaders(headers);
        body = body == null ? new byte[0] : body.clone();
    }

    public static Builder builder(final HttpMethod method, final URI uri) {
        return new Builder(method, uri);
    }

    public static Builder get(final URI uri) {
        return builder(HttpMethod.GET, uri);
    }

    public static Builder post(final URI uri) {
        return builder(HttpMethod.POST, uri);
    }

    public static Builder put(final URI uri) {
        return builder(HttpMethod.PUT, uri);
    }

    public static Builder patch(final URI uri) {
        return builder(HttpMethod.PATCH, uri);
    }

    public static Builder delete(final URI uri) {
        return builder(HttpMethod.DELETE, uri);
    }

    public String bodyAsString() {
        return new String(body, StandardCharsets.UTF_8);
    }

    private static Map<String, List<String>> normalizeHeaders(final Map<String, List<String>> raw) {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        final var copy = new LinkedHashMap<String, List<String>>();
        for (final var entry : raw.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Map.copyOf(copy);
    }

    public static final class Builder {
        private final HttpMethod method;
        private final URI uri;
        private final Map<String, List<String>> headers = new LinkedHashMap<>();
        private byte[] body;
        private Duration timeout;
        private String contentType;

        private Builder(final HttpMethod method, final URI uri) {
            this.method = method;
            this.uri = uri;
        }

        public Builder header(final String name, final String value) {
            headers.computeIfAbsent(name, key -> new ArrayList<>()).add(value);
            return this;
        }

        public Builder contentType(final String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder body(final byte[] body) {
            this.body = body == null ? null : body.clone();
            return this;
        }

        public Builder body(final String content) {
            this.body = content == null ? null : content.getBytes(StandardCharsets.UTF_8);
            return this;
        }

        public Builder jsonBody(final Object value) {
            this.body = JsonUtils.toJsonBytes(value);
            this.contentType = "application/json";
            return this;
        }

        public Builder jsonBody(final Object value, final dev.rafex.ether.json.JsonCodec codec) {
            this.body = (codec == null ? JsonUtils.codec() : codec).toJsonBytes(value);
            this.contentType = "application/json";
            return this;
        }

        public Builder timeout(final Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public HttpRequestSpec build() {
            if (contentType != null && !headers.containsKey("Content-Type")) {
                header("Content-Type", contentType);
            }
            return new HttpRequestSpec(method, uri, headers, body, timeout);
        }
    }
}
