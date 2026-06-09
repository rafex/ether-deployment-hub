package dev.rafex.ether.http.problem.model;

/*-
 * #%L
 * ether-http-problem
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
import java.util.LinkedHashMap;
import java.util.Map;

public record ProblemDetails(URI type, String title, int status, String detail, URI instance,
        Map<String, Object> properties) {

    public ProblemDetails {
        type = type == null ? URI.create("about:blank") : type;
        title = title == null || title.isBlank() ? "Unknown problem" : title;
        detail = detail == null ? "" : detail;
        properties = properties == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(properties));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ProblemDetails of(final int status, final String title, final String detail) {
        return builder().status(status).title(title).detail(detail).build();
    }

    public static final class Builder {
        private URI type = URI.create("about:blank");
        private String title;
        private int status;
        private String detail = "";
        private URI instance;
        private final Map<String, Object> properties = new LinkedHashMap<>();

        public Builder type(final URI type) {
            this.type = type;
            return this;
        }

        public Builder title(final String title) {
            this.title = title;
            return this;
        }

        public Builder status(final int status) {
            this.status = status;
            return this;
        }

        public Builder detail(final String detail) {
            this.detail = detail;
            return this;
        }

        public Builder instance(final URI instance) {
            this.instance = instance;
            return this;
        }

        public Builder property(final String key, final Object value) {
            this.properties.put(key, value);
            return this;
        }

        public ProblemDetails build() {
            return new ProblemDetails(type, title, status, detail, instance, properties);
        }
    }
}
