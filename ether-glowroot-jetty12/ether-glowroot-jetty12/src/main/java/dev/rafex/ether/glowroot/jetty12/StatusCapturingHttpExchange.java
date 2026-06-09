package dev.rafex.ether.glowroot.jetty12;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.glowroot.agent.api.Glowroot;

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
 * Package-private {@link HttpExchange} decorator that intercepts all response
 * methods to record the HTTP status code as a Glowroot transaction attribute.
 *
 * <p>
 * Adds two attributes on every response:
 * </p>
 * <ul>
 * <li>{@code http.status} — e.g. {@code "200"}, {@code "404"},
 * {@code "500"}</li>
 * <li>{@code http.status_class} — e.g. {@code "2xx"}, {@code "4xx"},
 * {@code "5xx"}</li>
 * </ul>
 */
final class StatusCapturingHttpExchange implements HttpExchange {

    private final HttpExchange delegate;

    StatusCapturingHttpExchange(final HttpExchange delegate) {
        this.delegate = delegate;
    }

    /* ── delegation: read-only methods ── */

    @Override
    public String method() {
        return delegate.method();
    }

    @Override
    public String path() {
        return delegate.path();
    }

    @Override
    public String pathParam(final String name) {
        return delegate.pathParam(name);
    }

    @Override
    public String queryFirst(final String name) {
        return delegate.queryFirst(name);
    }

    @Override
    public List<String> queryAll(final String name) {
        return delegate.queryAll(name);
    }

    @Override
    public Map<String, String> pathParams() {
        return delegate.pathParams();
    }

    @Override
    public Map<String, List<String>> queryParams() {
        return delegate.queryParams();
    }

    @Override
    public Set<String> allowedMethods() {
        return delegate.allowedMethods();
    }

    /* ── interception: response-writing methods ── */

    @Override
    public void json(final int status, final Object body) {
        recordStatus(status);
        delegate.json(status, body);
    }

    @Override
    public void text(final int status, final String body) {
        recordStatus(status);
        delegate.text(status, body);
    }

    @Override
    public void noContent(final int status) {
        recordStatus(status);
        delegate.noContent(status);
    }

    /**
     * Overridden to capture 405 and preserve the delegate's Allow-header logic.
     */
    @Override
    public void methodNotAllowed() {
        recordStatus(405);
        delegate.methodNotAllowed();
    }

    /**
     * Overridden to capture 204 and preserve the delegate's Allow-header logic.
     */
    @Override
    public void options() {
        recordStatus(204);
        delegate.options();
    }

    /* ── private helpers ── */

    private static void recordStatus(final int status) {
        try {
            Glowroot.addTransactionAttribute("http.status", String.valueOf(status));
            Glowroot.addTransactionAttribute("http.status_class", (status / 100) + "xx");
        } catch (final Throwable ignore) {
            // Glowroot agent not present; do not affect response
        }
    }
}
