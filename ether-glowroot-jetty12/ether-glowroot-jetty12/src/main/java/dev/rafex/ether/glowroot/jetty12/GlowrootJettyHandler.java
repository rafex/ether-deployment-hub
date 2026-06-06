package dev.rafex.ether.glowroot.jetty12;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
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

import dev.rafex.ether.http.jetty12.JettyAuthHandler;
import dev.rafex.ether.observability.core.request.RequestIdGenerator;

/**
 * Jetty-level {@link Handler.Wrapper} that provides comprehensive Glowroot APM
 * instrumentation in a single handler, designed for use with
 * {@link dev.rafex.ether.http.jetty12.JettyMiddleware}-based architectures.
 *
 * <p>
 * Combines all the capabilities of the ether-level middleware suite into one
 * Jetty handler, making it compatible with projects that use Jetty's
 * {@code Handler.Wrapper} chain rather than the ether {@code HttpExchange}
 * middleware model.
 * </p>
 *
 * <h2>What it instruments</h2>
 * <ul>
 * <li><b>Transaction type/name</b> — {@code "Web"} +
 * {@code "METHOD /normalized/path"}</li>
 * <li><b>Response status</b> — {@code http.status} and
 * {@code http.status_class}</li>
 * <li><b>Authenticated user</b> — {@code Glowroot.setTransactionUser()} via
 * configurable extractor</li>
 * <li><b>Request ID</b> — from a configurable header (e.g.
 * {@code X-Request-Id}), with optional UUID generation</li>
 * <li><b>Health check suppression</b> — raises slow-threshold to max for probe
 * paths</li>
 * <li><b>Per-route slow thresholds</b> — different thresholds per normalized
 * path</li>
 * <li><b>Error attributes</b> — {@code error} and {@code error.message} on
 * uncaught exceptions</li>
 * </ul>
 *
 * <h2>Usage (Jetty middleware / Kiwi-style registration)</h2>
 *
 * <pre>{@code
 * final var glowroot = GlowrootJettyHandler.builder().healthPath("/health").requestIdHeader("X-Request-Id")
 *         .defaultSlowThreshold(2_000).userExtractor(ctx -> ctx instanceof MyAuthContext a ? a.subject() : null)
 *         .build();
 *
 * middlewareRegistry.add(glowroot::wrap); // Kiwi
 * etherMiddlewares.add(glowroot::wrap); // ether JettyMiddleware
 * }</pre>
 */
public final class GlowrootJettyHandler extends Handler.Wrapper {

    private final Set<String> healthPaths;
    private final Map<String, Long> thresholdByNormalizedPath;
    private final long defaultThresholdMs;
    private final String requestIdHeader;
    private final RequestIdGenerator requestIdGenerator;
    private final Function<Object, String> userExtractor;

    private GlowrootJettyHandler(final Handler next, final Builder builder) {
        super(next);
        healthPaths = Set.copyOf(builder.healthPaths);
        thresholdByNormalizedPath = Map.copyOf(builder.thresholds);
        defaultThresholdMs = builder.defaultThresholdMs;
        requestIdHeader = builder.requestIdHeader;
        requestIdGenerator = builder.requestIdGenerator;
        userExtractor = builder.userExtractor;
    }

    /** Returns a new {@link Builder}. */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean handle(final Request request, final Response response, final Callback callback) throws Exception {
        final var method = request.getMethod();
        final var path = request.getHttpURI() != null ? request.getHttpURI().getPath() : null;
        final var normalized = PathNormalizer.normalize(path);

        // ── 1. Transaction identity ──────────────────────────────────────────
        try {
            Glowroot.setTransactionType("Web");
            Glowroot.setTransactionName(method + " " + normalized);
            Glowroot.addTransactionAttribute("http.method", method);
            Glowroot.addTransactionAttribute("http.path", path == null ? "unknown" : path);
            Glowroot.addTransactionAttribute("http.normalized_path", normalized);
        } catch (final Throwable ignore) {
        }

        // ── 2. Slow-threshold (health suppression or per-route) ──────────────
        if (path != null && healthPaths.contains(path)) {
            try {
                Glowroot.setTransactionSlowThreshold(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            } catch (final Throwable ignore) {
            }
        } else {
            final var threshold = thresholdByNormalizedPath.getOrDefault(normalized, defaultThresholdMs);
            if (threshold > 0) {
                try {
                    Glowroot.setTransactionSlowThreshold(threshold, TimeUnit.MILLISECONDS);
                } catch (final Throwable ignore) {
                }
            }
        }

        // ── 3. Request ID ────────────────────────────────────────────────────
        if (requestIdHeader != null) {
            try {
                var reqId = request.getHeaders().get(requestIdHeader);
                if ((reqId == null || reqId.isBlank()) && requestIdGenerator != null) {
                    reqId = requestIdGenerator.nextId();
                }
                if (reqId != null && !reqId.isBlank()) {
                    Glowroot.addTransactionAttribute("request.id", reqId);
                }
            } catch (final Throwable ignore) {
            }
        }

        // ── 4. Delegate to next handler ──────────────────────────────────────
        try {
            final var result = super.handle(request, response, callback);

            // ── 5. Response status (available after synchronous handler) ─────
            try {
                final var status = response.getStatus();
                if (status > 0) {
                    Glowroot.addTransactionAttribute("http.status", String.valueOf(status));
                    Glowroot.addTransactionAttribute("http.status_class", status / 100 + "xx");
                }
            } catch (final Throwable ignore) {
            }

            // ── 6. Authenticated user (set by JettyAuthHandler inside chain) ─
            if (userExtractor != null) {
                try {
                    final var ctx = request.getAttribute(JettyAuthHandler.REQ_ATTR_AUTH);
                    if (ctx != null) {
                        final var user = userExtractor.apply(ctx);
                        if (user != null && !user.isBlank()) {
                            Glowroot.setTransactionUser(user);
                            Glowroot.addTransactionAttribute("auth.user", user);
                        }
                    }
                } catch (final Throwable ignore) {
                }
            }

            return result;

        } catch (final Throwable t) {
            // ── 7. Uncaught exception attributes ─────────────────────────────
            try {
                Glowroot.addTransactionAttribute("error", t.getClass().getName());
                Glowroot.addTransactionAttribute("error.message", t.getMessage() == null ? "" : t.getMessage());
            } catch (final Throwable ignore) {
            }
            throw t;
        }
    }

    /* ── Builder ─────────────────────────────────────────────────────────── */

    public static final class Builder {

        private final Set<String> healthPaths = new HashSet<>();
        private final Map<String, Long> thresholds = new HashMap<>();
        private long defaultThresholdMs = 2_000L;
        private String requestIdHeader = null;
        private RequestIdGenerator requestIdGenerator = null;
        private Function<Object, String> userExtractor = null;

        private Builder() {
        }

        /**
         * Adds a path that should never appear in Glowroot's slow-transaction list
         * (e.g. Kubernetes liveness/readiness probes).
         */
        public Builder healthPath(final String path) {
            healthPaths.add(path);
            return this;
        }

        /** Adds multiple health-check paths at once. */
        public Builder healthPaths(final String... paths) {
            Collections.addAll(healthPaths, paths);
            return this;
        }

        /**
         * Registers a custom slow-threshold (in ms) for a specific normalized path. The
         * path should use placeholders: {@code "/api/export/:id"}.
         */
        public Builder slowThreshold(final String normalizedPath, final long thresholdMs) {
            thresholds.put(normalizedPath, thresholdMs);
            return this;
        }

        /**
         * Sets the default slow-threshold (ms) used for paths with no specific entry.
         * Defaults to {@code 2 000} ms.
         */
        public Builder defaultSlowThreshold(final long thresholdMs) {
            defaultThresholdMs = thresholdMs;
            return this;
        }

        /**
         * Enables request-ID capture from the given header name.
         *
         * @param header header to read (e.g. {@code "X-Request-Id"})
         */
        public Builder requestIdHeader(final String header) {
            requestIdHeader = header;
            requestIdGenerator = null;
            return this;
        }

        /**
         * Enables request-ID capture from the given header name, with automatic UUID
         * generation when the header is absent.
         */
        public Builder requestIdHeader(final String header, final boolean generateIfAbsent) {
            requestIdHeader = header;
            requestIdGenerator = generateIfAbsent ? new GlowrootRequestIdGenerator() : null;
            return this;
        }

        /**
         * Enables request-ID capture from the given header and delegates generation to
         * a {@link RequestIdGenerator} when the header is absent.
         */
        public Builder requestIdHeader(final String header, final RequestIdGenerator requestIdGenerator) {
            requestIdHeader = header;
            this.requestIdGenerator = requestIdGenerator;
            return this;
        }

        /**
         * Sets the function used to extract the transaction user from the auth-context
         * object stored in {@link JettyAuthHandler#REQ_ATTR_AUTH}.
         *
         * <p>
         * The function receives the raw {@code Object} that was passed to
         * {@link dev.rafex.ether.http.jetty12.TokenVerificationResult#ok(Object)} and
         * should return the user identifier string, or {@code null} to skip user
         * recording.
         * </p>
         *
         * <p>
         * Example for a custom {@code AuthContext} record:
         * </p>
         *
         * <pre>{@code
         * .userExtractor(ctx -> ctx instanceof MyAuthContext a ? a.subject() : null)
         * }</pre>
         */
        public Builder userExtractor(final Function<Object, String> extractor) {
            userExtractor = extractor;
            return this;
        }

        /**
         * Creates a {@link GlowrootJettyHandler} wrapping {@code next}.
         *
         * <p>
         * This method is designed to be used as a method reference, matching both the
         * {@link dev.rafex.ether.http.jetty12.JettyMiddleware} and any Kiwi-style
         * {@code Middleware} functional interfaces:
         * </p>
         *
         * <pre>{@code
         * middlewareRegistry.add(glowrootBuilder::wrap);
         * }</pre>
         */
        public GlowrootJettyHandler wrap(final Handler next) {
            return new GlowrootJettyHandler(next, this);
        }
    }
}
