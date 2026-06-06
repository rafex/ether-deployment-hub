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

/**
 * Middleware that sets the authenticated user on the Glowroot transaction.
 *
 * <p>
 * Calls {@link Glowroot#setTransactionUser(String)} with the value returned by
 * a configurable {@code userExtractor} function. If the extractor returns
 * {@code null} or blank, the user is not set.
 * </p>
 *
 * <p>
 * For Jetty-specific extraction from JWT auth context use
 * {@link GlowrootJettyExtractors#authUser()} to obtain the extractor:
 * </p>
 * 
 * <pre>{@code
 * middlewareRegistry.add(new GlowrootAuthUserMiddleware(GlowrootJettyExtractors.authUser()));
 * }</pre>
 *
 * <p>
 * Setting the user enables Glowroot's <em>"User recording"</em> — you can
 * search all slow traces for a specific user, invaluable for debugging
 * user-specific issues.
 * </p>
 */
public final class GlowrootAuthUserMiddleware implements Middleware {

    private final Function<HttpExchange, String> userExtractor;

    /**
     * Creates a middleware with the given user-extractor function.
     *
     * @param userExtractor function that derives the user identifier from an
     *                      {@link HttpExchange}; must not be {@code null}
     */
    public GlowrootAuthUserMiddleware(final Function<HttpExchange, String> userExtractor) {
        this.userExtractor = Objects.requireNonNull(userExtractor, "userExtractor must not be null");
    }

    @Override
    public HttpHandler wrap(final HttpHandler next) {
        return exchange -> {
            try {
                final var user = userExtractor.apply(exchange);
                if (user != null && !user.isBlank()) {
                    Glowroot.setTransactionUser(user);
                    Glowroot.addTransactionAttribute("auth.user", user);
                }
            } catch (final Throwable ignore) {
                // Extractor or Glowroot failure must never affect the request
            }
            return next.handle(exchange);
        };
    }
}
