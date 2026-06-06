package dev.rafex.ether.glowroot.jetty12;

import java.util.List;
import java.util.Map;
import java.util.Set;

/*-
 * #%L
 * ether-glowroot-jetty12
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

import dev.rafex.ether.http.core.HttpExchange;

/**
 * In-memory {@link HttpExchange} test double.
 *
 * <p>
 * Records the last status code and body written so assertions can verify what
 * the handler sent as a response.
 * </p>
 */
final class TestHttpExchange implements HttpExchange {

    private final String method;
    private final String path;
    private final Map<String, String> pathParams;
    private final Map<String, List<String>> queryParams;

    private int lastStatus = -1;
    private Object lastBody;
    private String lastText;

    TestHttpExchange(final String method, final String path) {
        this(method, path, Map.of(), Map.of());
    }

    TestHttpExchange(final String method, final String path, final Map<String, String> pathParams,
            final Map<String, List<String>> queryParams) {
        this.method = method;
        this.path = path;
        this.pathParams = pathParams;
        this.queryParams = queryParams;
    }

    /* ── HttpExchange implementation ── */

    @Override
    public String method() {
        return method;
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public String pathParam(final String name) {
        return pathParams.get(name);
    }

    @Override
    public String queryFirst(final String name) {
        final var values = queryParams.get(name);
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }

    @Override
    public List<String> queryAll(final String name) {
        return queryParams.getOrDefault(name, List.of());
    }

    @Override
    public Map<String, String> pathParams() {
        return pathParams;
    }

    @Override
    public Map<String, List<String>> queryParams() {
        return queryParams;
    }

    @Override
    public Set<String> allowedMethods() {
        return Set.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }

    @Override
    public void json(final int status, final Object body) {
        this.lastStatus = status;
        this.lastBody = body;
    }

    @Override
    public void text(final int status, final String body) {
        this.lastStatus = status;
        this.lastText = body;
    }

    @Override
    public void noContent(final int status) {
        this.lastStatus = status;
    }

    /* ── Test accessors ── */

    int lastStatus() {
        return lastStatus;
    }

    Object lastBody() {
        return lastBody;
    }

    String lastText() {
        return lastText;
    }
}
