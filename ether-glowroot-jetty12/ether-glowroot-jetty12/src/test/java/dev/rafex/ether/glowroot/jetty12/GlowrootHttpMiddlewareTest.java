package dev.rafex.ether.glowroot.jetty12;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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

@DisplayName("GlowrootHttpMiddleware")
class GlowrootHttpMiddlewareTest {

    private final GlowrootHttpMiddleware middleware = new GlowrootHttpMiddleware();

    @Test
    @DisplayName("delegates to the next handler")
    void delegatesToNext() throws Exception {
        final var called = new AtomicBoolean(false);
        final var wrapped = middleware.wrap(ex -> {
            called.set(true);
            return true;
        });

        wrapped.handle(new TestHttpExchange("GET", "/api/users"));

        assertTrue(called.get());
    }

    @Test
    @DisplayName("next handler receives the same exchange")
    void nextReceivesSameExchange() throws Exception {
        final var received = new AtomicReference<String>();
        final var exchange = new TestHttpExchange("POST", "/api/orders");

        final var wrapped = middleware.wrap(ex -> {
            received.set(ex.method() + " " + ex.path());
            return true;
        });
        wrapped.handle(exchange);

        assertEquals("POST /api/orders", received.get());
    }

    @Test
    @DisplayName("propagates exception from next handler")
    void propagatesException() {
        final var wrapped = middleware.wrap(ex -> {
            throw new RuntimeException("handler-error");
        });

        final var ex = assertThrows(RuntimeException.class, () -> wrapped.handle(new TestHttpExchange("GET", "/fail")));
        assertEquals("handler-error", ex.getMessage());
    }

    @Test
    @DisplayName("does not swallow checked exception from next handler")
    void propagatesCheckedException() {
        final var wrapped = middleware.wrap(ex -> {
            throw new Exception("checked-error");
        });

        final var ex = assertThrows(Exception.class, () -> wrapped.handle(new TestHttpExchange("GET", "/fail")));
        assertEquals("checked-error", ex.getMessage());
    }

    @Test
    @DisplayName("null path does not throw")
    void nullPathDoesNotThrow() throws Exception {
        final var wrapped = middleware.wrap(ex -> true);
        assertDoesNotThrow(() -> wrapped.handle(new TestHttpExchange("GET", null)));
    }

    @Test
    @DisplayName("returns the boolean result from the next handler")
    void returnsHandlerResult() throws Exception {
        final var wrappedTrue = middleware.wrap(ex -> true);
        final var wrappedFalse = middleware.wrap(ex -> false);

        assertTrue(wrappedTrue.handle(new TestHttpExchange("GET", "/ok")));
        assertFalse(wrappedFalse.handle(new TestHttpExchange("GET", "/ok")));
    }
}
