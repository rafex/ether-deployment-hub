package dev.rafex.ether.glowroot.jetty12;

import java.util.function.Function;

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
import dev.rafex.ether.http.jetty12.JettyAuthHandler;
import dev.rafex.ether.http.jetty12.exchange.JettyHttpExchange;

/**
 * Factory of Jetty-specific {@link HttpExchange} extractor functions for use
 * with {@link GlowrootAuthUserMiddleware} and
 * {@link GlowrootRequestIdMiddleware}.
 *
 * <p>
 * All extractors safely return {@code null} when the exchange is not a
 * {@link JettyHttpExchange} (e.g. during unit tests with a test double).
 * </p>
 *
 * <p>
 * Usage:
 * </p>
 * 
 * <pre>{@code
 * // Set authenticated user from JWT auth context
 * middlewareRegistry.add(new GlowrootAuthUserMiddleware(GlowrootJettyExtractors.authUser()));
 *
 * // Capture X-Request-Id header (generate if absent)
 * middlewareRegistry.add(new GlowrootRequestIdMiddleware(GlowrootJettyExtractors.xRequestId(), true));
 *
 * // Capture any custom header
 * middlewareRegistry.add(new GlowrootRequestIdMiddleware(GlowrootJettyExtractors.header("X-Correlation-Id"), false));
 * }</pre>
 */
public final class GlowrootJettyExtractors {

    private GlowrootJettyExtractors() {
    }

    /**
     * Returns an extractor that reads the authenticated user from the
     * {@link JettyAuthHandler#REQ_ATTR_AUTH} request attribute set by
     * {@link JettyAuthHandler} after successful JWT verification.
     *
     * <p>
     * The raw {@code context} object is converted via {@code toString()};
     * implementors of {@link dev.rafex.ether.http.jetty12.TokenVerifier} should
     * ensure the context object returns the user subject from {@code toString()}.
     * </p>
     *
     * @return extractor function that yields the authenticated user, or
     *         {@code null}
     */
    public static Function<HttpExchange, String> authUser() {
        return exchange -> {
            if (exchange instanceof final JettyHttpExchange jettyExchange) {
                final var ctx = jettyExchange.request().getAttribute(JettyAuthHandler.REQ_ATTR_AUTH);
                return ctx == null ? null : ctx.toString();
            }
            return null;
        };
    }

    /**
     * Returns an extractor that reads the value of a specific HTTP request header.
     *
     * @param headerName the header name (case-insensitive per HTTP spec)
     * @return extractor function that yields the header value, or {@code null} if
     *         absent
     */
    public static Function<HttpExchange, String> header(final String headerName) {
        return exchange -> {
            if (exchange instanceof final JettyHttpExchange jettyExchange) {
                return jettyExchange.request().getHeaders().get(headerName);
            }
            return null;
        };
    }

    /**
     * Returns an extractor for the {@code X-Request-Id} header, the de-facto
     * standard for HTTP request correlation.
     *
     * @return extractor function that yields the {@code X-Request-Id} header value,
     *         or {@code null}
     */
    public static Function<HttpExchange, String> xRequestId() {
        return header("X-Request-Id");
    }

    /**
     * Returns an extractor for the client IP address, preferring
     * {@code X-Forwarded-For} (set by reverse proxies / load-balancers) over the
     * direct remote address.
     *
     * @return extractor function that yields the client IP address, or {@code null}
     */
    public static Function<HttpExchange, String> clientIp() {
        return exchange -> {
            if (exchange instanceof final JettyHttpExchange jettyExchange) {
                final var forwarded = jettyExchange.request().getHeaders().get("X-Forwarded-For");
                if (forwarded != null && !forwarded.isBlank()) {
                    // X-Forwarded-For may contain a comma-separated list; first entry is the client
                    final int comma = forwarded.indexOf(',');
                    return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
                }
                final var addr = jettyExchange.request().getConnectionMetaData().getRemoteSocketAddress();
                return addr == null ? null : addr.toString();
            }
            return null;
        };
    }
}
