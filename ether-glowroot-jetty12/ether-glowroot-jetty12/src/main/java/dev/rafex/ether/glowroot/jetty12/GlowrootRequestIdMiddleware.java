package dev.rafex.ether.glowroot.jetty12;

import java.util.Objects;
import java.util.function.Function;

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
import dev.rafex.ether.http.core.HttpHandler;
import dev.rafex.ether.http.core.Middleware;
import dev.rafex.ether.observability.core.request.RequestIdGenerator;

/**
 * Middleware that records a correlation request ID as a Glowroot attribute.
 *
 * <p>
 * Extracts the request ID via a configurable {@code idExtractor} function. If
 * no ID is found and {@code generateIfAbsent} is {@code true}, a random UUID is
 * generated. The resulting ID is stored under the {@code request.id}
 * transaction attribute, enabling correlation between Glowroot traces and
 * external log aggregators (Loki, ELK, etc.).
 * </p>
 *
 * <p>
 * For Jetty-specific extraction from the {@code X-Request-Id} header use
 * {@link GlowrootJettyExtractors#xRequestId()} as the extractor:
 * </p>
 * 
 * <pre>{@code
 * middlewareRegistry.add(new GlowrootRequestIdMiddleware(GlowrootJettyExtractors.xRequestId(), true));
 * }</pre>
 */
public final class GlowrootRequestIdMiddleware implements Middleware {

    private final Function<HttpExchange, String> idExtractor;
    private final RequestIdGenerator requestIdGenerator;

    public GlowrootRequestIdMiddleware(final Function<HttpExchange, String> idExtractor,
            final boolean generateIfAbsent) {
        this(idExtractor, generateIfAbsent ? new GlowrootRequestIdGenerator() : null);
    }

    public GlowrootRequestIdMiddleware(final Function<HttpExchange, String> idExtractor,
            final RequestIdGenerator requestIdGenerator) {
        this.idExtractor = Objects.requireNonNull(idExtractor, "idExtractor must not be null");
        this.requestIdGenerator = requestIdGenerator;
    }

    @Override
    public HttpHandler wrap(final HttpHandler next) {
        return exchange -> {
            try {
                String requestId = idExtractor.apply(exchange);
                if ((requestId == null || requestId.isBlank()) && requestIdGenerator != null) {
                    requestId = requestIdGenerator.nextId();
                }
                if (requestId != null && !requestId.isBlank()) {
                    Glowroot.addTransactionAttribute("request.id", requestId);
                }
            } catch (final Throwable ignore) {
                // Extractor or Glowroot failure must never affect the request
            }
            return next.handle(exchange);
        };
    }
}
