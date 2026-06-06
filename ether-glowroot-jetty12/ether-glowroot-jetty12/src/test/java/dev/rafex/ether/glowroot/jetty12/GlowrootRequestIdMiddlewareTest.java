package dev.rafex.ether.glowroot.jetty12;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

import dev.rafex.ether.observability.core.request.RequestIdGenerator;

@DisplayName("GlowrootRequestIdMiddleware")
class GlowrootRequestIdMiddlewareTest {

    /* ── constructor guard ── */

    @Test
    @DisplayName("null extractor throws NullPointerException")
    void nullExtractorThrows() {
        assertThrows(NullPointerException.class, () -> new GlowrootRequestIdMiddleware(null, false));
    }

    /* ── happy path ── */

    @Test
    @DisplayName("delegates to next handler when request ID is found")
    void delegatesWhenIdPresent() throws Exception {
        final var called = new AtomicBoolean(false);
        final var middleware = new GlowrootRequestIdMiddleware(ex -> "req-abc123", false);
        final var wrapped = middleware.wrap(ex -> {
            called.set(true);
            return true;
        });

        wrapped.handle(new TestHttpExchange("GET", "/api/items"));
        assertTrue(called.get());
    }

    @Test
    @DisplayName("delegates to next handler when extractor returns null and generateIfAbsent=false")
    void delegatesWhenNoIdNoGenerate() throws Exception {
        final var called = new AtomicBoolean(false);
        final var middleware = new GlowrootRequestIdMiddleware(ex -> null, false);
        final var wrapped = middleware.wrap(ex -> {
            called.set(true);
            return true;
        });

        wrapped.handle(new TestHttpExchange("GET", "/api/items"));
        assertTrue(called.get());
    }

    @Test
    @DisplayName("generateIfAbsent=true: generates UUID when extractor returns null")
    void generatesUuidWhenAbsent() throws Exception {
        final var called = new AtomicBoolean(false);
        final var middleware = new GlowrootRequestIdMiddleware(ex -> null, true);
        final var wrapped = middleware.wrap(ex -> {
            called.set(true);
            return true;
        });

        // Should not throw; UUID is generated silently
        assertDoesNotThrow(() -> wrapped.handle(new TestHttpExchange("GET", "/api/items")));
        assertTrue(called.get());
    }

    @Test
    @DisplayName("generateIfAbsent=true: skips generation when extractor returns a value")
    void doesNotGenerateWhenIdPresent() throws Exception {
        final var called = new AtomicBoolean(false);
        final var middleware = new GlowrootRequestIdMiddleware(ex -> "existing-id", true);
        final var wrapped = middleware.wrap(ex -> {
            called.set(true);
            return true;
        });

        wrapped.handle(new TestHttpExchange("GET", "/api/items"));
        assertTrue(called.get());
    }

    /* ── extractor exception safety ── */

    @Test
    @DisplayName("extractor exception is silenced — next handler still called")
    void extractorExceptionIsSilenced() throws Exception {
        final var called = new AtomicBoolean(false);
        final var middleware = new GlowrootRequestIdMiddleware(ex -> {
            throw new RuntimeException("extractor-fail");
        }, false);
        final var wrapped = middleware.wrap(ex -> {
            called.set(true);
            return true;
        });

        assertDoesNotThrow(() -> wrapped.handle(new TestHttpExchange("GET", "/api/items")));
        assertTrue(called.get());
    }

    /* ── blank values ── */

    @Test
    @DisplayName("blank request ID from extractor does not throw")
    void blankIdDoesNotThrow() throws Exception {
        final var middleware = new GlowrootRequestIdMiddleware(ex -> "   ", false);
        final var wrapped = middleware.wrap(ex -> true);
        assertDoesNotThrow(() -> wrapped.handle(new TestHttpExchange("GET", "/api/items")));
    }

    /* ── next handler exceptions ── */

    @Test
    @DisplayName("exception from next handler is propagated")
    void nextHandlerExceptionPropagates() {
        final var middleware = new GlowrootRequestIdMiddleware(ex -> "id-1", false);
        final var wrapped = middleware.wrap(ex -> {
            throw new RuntimeException("handler-fail");
        });

        assertThrows(RuntimeException.class, () -> wrapped.handle(new TestHttpExchange("GET", "/api/items")));
    }

    @Test
    @DisplayName("delegates generation to a RequestIdGenerator when configured")
    void delegatesGenerationToRequestIdGenerator() throws Exception {
        final var called = new AtomicBoolean(false);
        final RequestIdGenerator generator = () -> "generated-id";
        final var middleware = new GlowrootRequestIdMiddleware(ex -> null, generator);
        final var wrapped = middleware.wrap(ex -> {
            called.set(true);
            return true;
        });

        assertDoesNotThrow(() -> wrapped.handle(new TestHttpExchange("GET", "/api/items")));
        assertTrue(called.get());
    }
}
