package dev.rafex.ether.glowroot.jetty12;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
 * Middleware that applies per-route slow-transaction thresholds in Glowroot.
 *
 * <p>
 * Different endpoints have different latency characteristics — a PDF export is
 * naturally slower than an auth check. This middleware lets you specify
 * distinct thresholds so Glowroot's slow-trace alerts are meaningful for every
 * route, not just a single global value.
 * </p>
 *
 * <p>
 * Route matching uses {@link PathNormalizer} so patterns with path variables
 * (e.g. {@code /users/:id}) match any concrete URL like
 * {@code /users/550e8400-…}.
 * </p>
 *
 * <p>
 * Build with the fluent {@link Builder}:
 * </p>
 * 
 * <pre>{@code
 * middlewareRegistry.add(GlowrootSlowThresholdMiddleware.builder().defaultThreshold(2_000) // 2 s for everything else
 *         .threshold("/api/export/:id", 30_000) // 30 s for exports
 *         .threshold("/api/auth/login", 500) // 500 ms for login
 *         .build());
 * }</pre>
 */
public final class GlowrootSlowThresholdMiddleware implements Middleware {

    private final Map<String, Long> thresholdByNormalizedPath;
    private final long defaultThresholdMs;

    private GlowrootSlowThresholdMiddleware(final Map<String, Long> thresholds, final long defaultThresholdMs) {
        this.thresholdByNormalizedPath = Map.copyOf(thresholds);
        this.defaultThresholdMs = defaultThresholdMs;
    }

    /** Returns a new {@link Builder}. */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public HttpHandler wrap(final HttpHandler next) {
        return exchange -> {
            final var normalized = PathNormalizer.normalize(exchange.path());
            final var threshold = thresholdByNormalizedPath.getOrDefault(normalized, defaultThresholdMs);
            if (threshold > 0) {
                try {
                    Glowroot.setTransactionSlowThreshold(threshold, TimeUnit.MILLISECONDS);
                } catch (final Throwable ignore) {
                    // Glowroot agent not present; do not affect request
                }
            }
            return next.handle(exchange);
        };
    }

    /* ── Builder ── */

    public static final class Builder {

        private final Map<String, Long> thresholds = new HashMap<>();
        private long defaultThresholdMs = 2_000L;

        private Builder() {
        }

        /**
         * Registers a slow threshold for an exact normalized path.
         *
         * @param normalizedPath path with placeholders, e.g. {@code "/users/:id"}
         * @param thresholdMs    threshold in milliseconds
         */
        public Builder threshold(final String normalizedPath, final long thresholdMs) {
            thresholds.put(normalizedPath, thresholdMs);
            return this;
        }

        /**
         * Sets the default threshold applied to paths not explicitly configured.
         * Default is {@code 2000} ms.
         */
        public Builder defaultThreshold(final long thresholdMs) {
            this.defaultThresholdMs = thresholdMs;
            return this;
        }

        public GlowrootSlowThresholdMiddleware build() {
            return new GlowrootSlowThresholdMiddleware(thresholds, defaultThresholdMs);
        }
    }
}
