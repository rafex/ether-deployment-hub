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

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record HttpResponseSpec(int statusCode, Map<String, List<String>> headers, byte[] body) {

    public HttpResponseSpec {
        headers = normalizeHeaders(headers);
        body = body == null ? new byte[0] : body.clone();
    }

    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
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
}
