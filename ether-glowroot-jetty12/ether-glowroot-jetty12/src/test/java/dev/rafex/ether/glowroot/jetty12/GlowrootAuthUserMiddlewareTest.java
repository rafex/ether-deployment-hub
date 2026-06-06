package dev.rafex.ether.glowroot.jetty12;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GlowrootAuthUserMiddleware")
class GlowrootAuthUserMiddlewareTest {

    /* ── constructor guard ── */

    @Test
    @DisplayName("null extractor throws NullPointerException")
    void nullExtractorThrows() {
        assertThrows(NullPointerException.class, () -> new GlowrootAuthUserMiddleware(null));
    }

    /* ── happy path ── */

    @Test
    @DisplayName("delegates to the next handler when user is present")
    void delegatesWithUser() throws Exception {
        final var called = new AtomicBoolean(false);
        final var middleware = new GlowrootAuthUserMiddleware(ex -> "user-42");
        final var wrapped = middleware.wrap(ex -> {
            called.set(true);
            return true;
        });

        wrapped.handle(new TestHttpExchange("GET", "/api/profile"));
        assertTrue(called.get());
    }

    @Test
    @DisplayName("delegates to the next handler when extractor returns null")
    void delegatesWithNullUser() throws Exception {
        final var called = new AtomicBoolean(false);
        final var middleware = new GlowrootAuthUserMiddleware(ex -> null);
        final var wrapped = middleware.wrap(ex -> {
            called.set(true);
            return true;
        });

        wrapped.handle(new TestHttpExchange("GET", "/api/public"));
        assertTrue(called.get());
    }

    @Test
    @DisplayName("delegates to the next handler when extractor returns blank")
    void delegatesWithBlankUser() throws Exception {
        final var called = new AtomicBoolean(false);
        final var middleware = new GlowrootAuthUserMiddleware(ex -> "   ");
        final var wrapped = middleware.wrap(ex -> {
            called.set(true);
            return true;
        });

        wrapped.handle(new TestHttpExchange("GET", "/api/public"));
        assertTrue(called.get());
    }

    /* ── extractor exception safety ── */

    @Test
    @DisplayName("extractor exception does not propagate — next handler is still called")
    void extractorExceptionIsSilenced() throws Exception {
        final var called = new AtomicBoolean(false);
        final var middleware = new GlowrootAuthUserMiddleware(ex -> {
            throw new RuntimeException("extractor-boom");
        });
        final var wrapped = middleware.wrap(ex -> {
            called.set(true);
            return true;
        });

        // Must NOT throw; the extractor failure is silenced
        assertDoesNotThrow(() -> wrapped.handle(new TestHttpExchange("GET", "/api/data")));
        assertTrue(called.get());
    }

    /* ── next handler exceptions ── */

    @Test
    @DisplayName("exception from next handler is propagated")
    void nextHandlerExceptionPropagates() {
        final var middleware = new GlowrootAuthUserMiddleware(ex -> "user-1");
        final var wrapped = middleware.wrap(ex -> {
            throw new RuntimeException("handler-boom");
        });

        assertThrows(RuntimeException.class, () -> wrapped.handle(new TestHttpExchange("GET", "/api/data")));
    }

    /* ── return value ── */

    @Test
    @DisplayName("returns boolean from the next handler")
    void returnsNextResult() throws Exception {
        final var middlewareTrue = new GlowrootAuthUserMiddleware(ex -> "u");
        final var middlewareFalse = new GlowrootAuthUserMiddleware(ex -> "u");

        assertTrue(middlewareTrue.wrap(ex -> true).handle(new TestHttpExchange("GET", "/x")));
        assertFalse(middlewareFalse.wrap(ex -> false).handle(new TestHttpExchange("GET", "/x")));
    }
}
