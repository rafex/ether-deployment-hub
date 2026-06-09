package dev.rafex.ether.http.client.config;

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

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record HttpClientConfig(Duration connectTimeout, Duration requestTimeout, HttpClient.Redirect redirectPolicy,
        String userAgent, Map<String, List<String>> defaultHeaders) {

    public static Builder builder() {
        return new Builder();
    }

    public static HttpClientConfig defaults() {
        return builder().build();
    }

    public Map<String, List<String>> defaultHeaders() {
        return Map.copyOf(defaultHeaders);
    }

    public static final class Builder {
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration requestTimeout = Duration.ofSeconds(30);
        private HttpClient.Redirect redirectPolicy = HttpClient.Redirect.NORMAL;
        private String userAgent = "ether-http-client/1.0";
        private final Map<String, List<String>> defaultHeaders = new LinkedHashMap<>();

        public Builder connectTimeout(final Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder requestTimeout(final Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
            return this;
        }

        public Builder redirectPolicy(final HttpClient.Redirect redirectPolicy) {
            this.redirectPolicy = redirectPolicy;
            return this;
        }

        public Builder userAgent(final String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder defaultHeader(final String name, final String value) {
            defaultHeaders.computeIfAbsent(name, key -> new java.util.ArrayList<>()).add(value);
            return this;
        }

        public HttpClientConfig build() {
            return new HttpClientConfig(connectTimeout, requestTimeout, redirectPolicy, userAgent,
                    defaultHeaders.entrySet().stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,
                            entry -> List.copyOf(entry.getValue()), (left, right) -> left, LinkedHashMap::new)));
        }
    }
}
