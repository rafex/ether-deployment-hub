package dev.rafex.ether.glowroot.jetty12;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

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

@DisplayName("GlowrootStatusCapturingMiddleware + StatusCapturingHttpExchange")
class GlowrootStatusCapturingMiddlewareTest {

    private final GlowrootStatusCapturingMiddleware middleware = new GlowrootStatusCapturingMiddleware();

    /* ── delegation through response methods ── */

    @Test
    @DisplayName("json() is delegated to the underlying exchange")
    void jsonDelegated() throws Exception {
        final var exchange = new TestHttpExchange("GET", "/api/items");
        final var wrapped = middleware.wrap(ex -> {
            ex.json(200, Map.of("ok", true));
            return true;
        });

        wrapped.handle(exchange);

        assertEquals(200, exchange.lastStatus());
        assertNotNull(exchange.lastBody());
    }

    @Test
    @DisplayName("text() is delegated to the underlying exchange")
    void textDelegated() throws Exception {
        final var exchange = new TestHttpExchange("GET", "/ping");
        final var wrapped = middleware.wrap(ex -> {
            ex.text(200, "pong");
            return true;
        });

        wrapped.handle(exchange);

        assertEquals(200, exchange.lastStatus());
        assertEquals("pong", exchange.lastText());
    }

    @Test
    @DisplayName("noContent() is delegated to the underlying exchange")
    void noContentDelegated() throws Exception {
        final var exchange = new TestHttpExchange("DELETE", "/api/items/1");
        final var wrapped = middleware.wrap(ex -> {
            ex.noContent(204);
            return true;
        });

        wrapped.handle(exchange);

        assertEquals(204, exchange.lastStatus());
    }

    @Test
    @DisplayName("methodNotAllowed() is delegated to the underlying exchange")
    void methodNotAllowedDelegated() throws Exception {
        final var exchange = new TestHttpExchange("PATCH", "/api/items");
        final var wrapped = middleware.wrap(ex -> {
            ex.methodNotAllowed();
            return true;
        });

        wrapped.handle(exchange);

        assertEquals(405, exchange.lastStatus());
    }

    @Test
    @DisplayName("options() is delegated to the underlying exchange")
    void optionsDelegated() throws Exception {
        final var exchange = new TestHttpExchange("OPTIONS", "/api/items");
        final var wrapped = middleware.wrap(ex -> {
            ex.options();
            return true;
        });

        wrapped.handle(exchange);

        assertEquals(204, exchange.lastStatus());
    }

    /* ── read-only delegation ── */

    @Test
    @DisplayName("method() is delegated transparently")
    void methodDelegated() throws Exception {
        final var wrapped = middleware.wrap(ex -> {
            assertEquals("PUT", ex.method());
            return true;
        });
        wrapped.handle(new TestHttpExchange("PUT", "/api/x"));
    }

    @Test
    @DisplayName("path() is delegated transparently")
    void pathDelegated() throws Exception {
        final var wrapped = middleware.wrap(ex -> {
            assertEquals("/api/things", ex.path());
            return true;
        });
        wrapped.handle(new TestHttpExchange("GET", "/api/things"));
    }

    /* ── error path ── */

    @Test
    @DisplayName("exception from next handler is propagated")
    void exceptionPropagated() {
        final var wrapped = middleware.wrap(ex -> {
            throw new RuntimeException("boom");
        });

        assertThrows(RuntimeException.class, () -> wrapped.handle(new TestHttpExchange("GET", "/api/items")));
    }

    /* ── StatusCapturingHttpExchange: multiple writes ── */

    @Test
    @DisplayName("last status wins when handler writes twice (atypical but safe)")
    void lastStatusWins() throws Exception {
        final var exchange = new TestHttpExchange("GET", "/api/items");
        final var wrapped = middleware.wrap(ex -> {
            ex.json(200, "first");
            ex.json(500, "second");
            return true;
        });

        wrapped.handle(exchange);

        // The underlying delegate's lastStatus reflects the second call
        assertEquals(500, exchange.lastStatus());
    }
}
