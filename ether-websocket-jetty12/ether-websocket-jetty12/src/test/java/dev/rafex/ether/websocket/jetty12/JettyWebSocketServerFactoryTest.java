package dev.rafex.ether.websocket.jetty12;

/*-
 * #%L
 * ether-websocket-jetty12
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.rafex.ether.websocket.core.WebSocketEndpoint;

class JettyWebSocketServerFactoryTest {

    private static JettyWebSocketServerConfig config() {
        return new JettyWebSocketServerConfig(0, 2, 8, 10_000, "ether-ws-test");
    }

    private static final class EchoEndpoint implements WebSocketEndpoint {
    }

    // ── create(config, registry) ──────────────────────────────────────────────

    @Test
    void createWithNullConfigThrows() {
        assertThrows(NullPointerException.class,
                () -> JettyWebSocketServerFactory.create(null, new JettyWebSocketRouteRegistry()));
    }

    @Test
    void createWithNullRegistryThrows() {
        assertThrows(NullPointerException.class,
                () -> JettyWebSocketServerFactory.create(config(), (JettyWebSocketRouteRegistry) null));
    }

    @Test
    void createWithEmptyRegistryProducesRunner() {
        final var runner = JettyWebSocketServerFactory.create(config(), new JettyWebSocketRouteRegistry());
        assertNotNull(runner);
        assertNotNull(runner.server());
    }

    @Test
    void createWithRegisteredRouteProducesRunner() {
        final var registry = new JettyWebSocketRouteRegistry();
        registry.add("/ws/echo", new EchoEndpoint());
        final var runner = JettyWebSocketServerFactory.create(config(), registry);
        assertNotNull(runner.server());
    }

    // ── create(config, modules) ───────────────────────────────────────────────

    @Test
    void createWithNullModulesDoesNotThrow() {
        final var runner = JettyWebSocketServerFactory.create(config(), (List<JettyWebSocketModule>) null);
        assertNotNull(runner);
        assertNotNull(runner.server());
    }

    @Test
    void createWithEmptyModulesDoesNotThrow() {
        final var runner = JettyWebSocketServerFactory.create(config(), List.<JettyWebSocketModule>of());
        assertNotNull(runner.server());
    }

    @Test
    void createInvokesModuleRegisterRoutes() {
        final boolean[] invoked = {false};
        final JettyWebSocketModule module = new JettyWebSocketModule() {
            @Override
            public void registerRoutes(final JettyWebSocketRouteRegistry registry,
                    final JettyWebSocketModuleContext ctx) {
                invoked[0] = true;
                registry.add("/ws/module", new EchoEndpoint());
            }
        };
        final var runner = JettyWebSocketServerFactory.create(config(), List.of(module));
        assertNotNull(runner.server());
        assertTrue(invoked[0], "module.registerRoutes should have been called");
    }

    // ── lifecycle (server can start and stop on ephemeral port) ────────────────

    @Test
    void serverStartsAndStops() {
        final var registry = new JettyWebSocketRouteRegistry();
        registry.add("/ws/echo", new EchoEndpoint());
        final var runner = JettyWebSocketServerFactory.create(config(), registry);
        assertDoesNotThrow(() -> {
            runner.start();
            runner.stop();
        });
    }
}
