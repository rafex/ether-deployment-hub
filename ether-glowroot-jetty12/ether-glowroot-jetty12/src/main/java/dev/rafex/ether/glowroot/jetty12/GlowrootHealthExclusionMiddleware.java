package dev.rafex.ether.glowroot.jetty12;

import java.util.Set;
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
 * Middleware that suppresses Glowroot slow-transaction alerts for
 * infrastructure paths such as health checks, readiness probes, and metrics.
 *
 * <p>
 * For matching paths the slow-transaction threshold is raised to
 * {@link Long#MAX_VALUE} milliseconds, so they never appear in Glowroot's
 * slow-trace list. They will still be recorded normally; only the <em>slow</em>
 * flag is suppressed.
 * </p>
 *
 * <p>
 * Use {@link #defaults()} for the most common Kubernetes/Docker health
 * endpoints, or {@link #of(String...)} to specify your own set:
 * </p>
 * 
 * <pre>{@code
 * // Default paths: /health, /ready, /live, /metrics
 * middlewareRegistry.add(GlowrootHealthExclusionMiddleware.defaults());
 *
 * // Custom paths
 * middlewareRegistry.add(GlowrootHealthExclusionMiddleware.of("/ping", "/status"));
 * }</pre>
 */
public final class GlowrootHealthExclusionMiddleware implements Middleware {

    /** Paths used by {@link #defaults()}. */
    public static final Set<String> DEFAULT_PATHS = Set.of("/health", "/ready", "/live", "/metrics");

    private final Set<String> excludedPaths;

    private GlowrootHealthExclusionMiddleware(final Set<String> excludedPaths) {
        this.excludedPaths = Set.copyOf(excludedPaths);
    }

    /**
     * Creates an instance with the given exact paths.
     *
     * @param paths paths to suppress (e.g. {@code "/ping"}, {@code "/status"})
     * @return a new {@link GlowrootHealthExclusionMiddleware} for those paths
     */
    public static GlowrootHealthExclusionMiddleware of(final String... paths) {
        return new GlowrootHealthExclusionMiddleware(Set.of(paths));
    }

    /** Creates an instance excluding {@link #DEFAULT_PATHS}. */
    public static GlowrootHealthExclusionMiddleware defaults() {
        return new GlowrootHealthExclusionMiddleware(DEFAULT_PATHS);
    }

    @Override
    public HttpHandler wrap(final HttpHandler next) {
        return exchange -> {
            final var path = exchange.path();
            if (path != null && excludedPaths.contains(path)) {
                try {
                    Glowroot.setTransactionSlowThreshold(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                } catch (final Throwable ignore) {
                    // Glowroot agent not present; do not affect request
                }
            }
            return next.handle(exchange);
        };
    }
}
