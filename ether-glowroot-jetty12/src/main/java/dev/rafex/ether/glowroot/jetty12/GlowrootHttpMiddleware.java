package dev.rafex.ether.glowroot.jetty12;

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

import dev.rafex.ether.http.core.HttpHandler;
import dev.rafex.ether.http.core.Middleware;

/**
 * Middleware that instruments HTTP requests with Glowroot APM.
 *
 * <p>
 * Sets the Glowroot transaction type to {@code "Web"} and names each
 * transaction as {@code "METHOD /normalized/path"}, replacing dynamic path
 * segments (UUIDs, ObjectIds, numeric IDs) with canonical placeholders so that
 * Glowroot can aggregate similar endpoints.
 * </p>
 *
 * <p>
 * Usage — register once when building the Jetty server:
 * </p>
 * 
 * <pre>{@code
 * middlewareRegistry.add(new GlowrootHttpMiddleware());
 * }</pre>
 */
public final class GlowrootHttpMiddleware implements Middleware {

    @Override
    public HttpHandler wrap(final HttpHandler next) {
        return exchange -> {
            final var method = exchange.method();
            final var path = exchange.path();
            final var normalized = PathNormalizer.normalize(path);

            try {
                Glowroot.setTransactionType("Web");
                Glowroot.setTransactionName(method + " " + normalized);
                Glowroot.addTransactionAttribute("http.method", method);
                Glowroot.addTransactionAttribute("http.path", path == null ? "unknown" : path);
                Glowroot.addTransactionAttribute("http.normalized_path", normalized);
            } catch (final Throwable ignore) {
                // Glowroot agent not present; do not affect the request
            }

            try {
                return next.handle(exchange);
            } catch (final Throwable t) {
                try {
                    Glowroot.addTransactionAttribute("error", t.getClass().getName());
                    Glowroot.addTransactionAttribute("error.message", t.getMessage() == null ? "" : t.getMessage());
                } catch (final Throwable ignore) {
                    // Glowroot agent not present; do not suppress original exception
                }
                throw t;
            }
        };
    }
}
