package dev.rafex.ether.glowroot.jetty12;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GlowrootJettyHandler}.
 *
 * <p>
 * The Glowroot agent is not attached during tests, so every {@code Glowroot.*}
 * call in the handler is wrapped in {@code try/catch(Throwable)} and silently
 * no-ops. These tests therefore verify the <b>handler wiring</b>: that it
 * delegates to the next handler, propagates its result and exceptions, and does
 * not throw on any path / builder configuration.
 * </p>
 */
@DisplayName("GlowrootJettyHandler")
class GlowrootJettyHandlerTest {

    /** Handler that records the request it received and returns a fixed status. */
    private static final class RecordingHandler extends Handler.Abstract {

        private final int status;
        private final AtomicReference<String> seen = new AtomicReference<>();

        RecordingHandler(final int status) {
            this.status = status;
        }

        @Override
        public boolean handle(final Request request, final Response response, final Callback callback) {
            seen.set(request.getMethod() + " " + request.getHttpURI().getPath());
            response.setStatus(status);
            callback.succeeded();
            return true;
        }
    }

    /** Handler that always throws, to verify exception propagation. */
    private static final class ThrowingHandler extends Handler.Abstract {

        @Override
        public boolean handle(final Request request, final Response response, final Callback callback) {
            throw new RuntimeException("handler-boom");
        }
    }

    private static Server startServer(final Handler handler) throws Exception {
        final var server = new Server();
        final var connector = new ServerConnector(server);
        connector.setPort(0);
        connector.setHost("127.0.0.1");
        server.addConnector(connector);
        server.setHandler(handler);
        server.start();
        return server;
    }

    private static HttpResponse<String> get(final Server server, final String path) throws Exception {
        final var connector = (ServerConnector) server.getConnectors()[0];
        final var request = HttpRequest
                .newBuilder(URI.create("http://127.0.0.1:" + connector.getLocalPort() + path)).GET().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> get(final Server server, final String path, final String headerName,
            final String headerValue) throws Exception {
        final var connector = (ServerConnector) server.getConnectors()[0];
        final var request = HttpRequest
                .newBuilder(URI.create("http://127.0.0.1:" + connector.getLocalPort() + path))
                .header(headerName, headerValue).GET().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("delegates to the next handler")
    void delegatesToNextHandler() throws Exception {
        final var next = new RecordingHandler(200);
        final var handler = GlowrootJettyHandler.builder().wrap(next);
        final var server = startServer(handler);
        try {
            final var response = get(server, "/api/users");
            assertEquals(200, response.statusCode());
            assertEquals("GET /api/users", next.seen.get());
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("builder produces a usable handler wrapping next")
    void builderProducesHandler() {
        final var handler = GlowrootJettyHandler.builder().wrap(new RecordingHandler(200));
        assertNotNull(handler);
    }

    @Test
    @DisplayName("health path does not throw and still delegates")
    void healthPathDelegates() throws Exception {
        final var next = new RecordingHandler(200);
        final var handler = GlowrootJettyHandler.builder().healthPath("/health").wrap(next);
        final var server = startServer(handler);
        try {
            final var response = get(server, "/health");
            assertEquals(200, response.statusCode());
            assertEquals("GET /health", next.seen.get());
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("per-route slow threshold does not affect delegation")
    void perRouteThresholdDelegates() throws Exception {
        final var next = new RecordingHandler(201);
        final var handler = GlowrootJettyHandler.builder().slowThreshold("/api/export", 5_000L)
                .defaultSlowThreshold(1_000L).wrap(next);
        final var server = startServer(handler);
        try {
            final var response = get(server, "/api/export");
            assertEquals(201, response.statusCode());
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("request id from header does not throw")
    void requestIdFromHeader() throws Exception {
        final var next = new RecordingHandler(200);
        final var handler = GlowrootJettyHandler.builder().requestIdHeader("X-Request-Id").wrap(next);
        final var server = startServer(handler);
        try {
            final var response = get(server, "/api/orders", "X-Request-Id", "req-123");
            assertEquals(200, response.statusCode());
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("request id generated when absent does not throw")
    void requestIdGeneratedWhenAbsent() throws Exception {
        final var next = new RecordingHandler(200);
        final var handler = GlowrootJettyHandler.builder().requestIdHeader("X-Request-Id", true).wrap(next);
        final var server = startServer(handler);
        try {
            final var response = get(server, "/api/orders");
            assertEquals(200, response.statusCode());
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("user extractor does not throw when no auth context present")
    void userExtractorWithoutAuthContext() throws Exception {
        final var next = new RecordingHandler(200);
        final var handler = GlowrootJettyHandler.builder()
                .userExtractor(ctx -> ctx == null ? null : ctx.toString()).wrap(next);
        final var server = startServer(handler);
        try {
            final var response = get(server, "/secure");
            assertEquals(200, response.statusCode());
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("captures response status class without altering it")
    void capturesStatusClass() throws Exception {
        final var next = new RecordingHandler(404);
        final var handler = GlowrootJettyHandler.builder().wrap(next);
        final var server = startServer(handler);
        try {
            final var response = get(server, "/missing");
            assertEquals(404, response.statusCode());
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("propagates exception from next handler")
    void propagatesException() throws Exception {
        final var handler = GlowrootJettyHandler.builder().wrap(new ThrowingHandler());
        final var server = startServer(handler);
        try {
            final var response = get(server, "/fail");
            assertTrue(response.statusCode() >= 500, "expected a 5xx after handler throws");
        } finally {
            server.stop();
        }
    }
}
