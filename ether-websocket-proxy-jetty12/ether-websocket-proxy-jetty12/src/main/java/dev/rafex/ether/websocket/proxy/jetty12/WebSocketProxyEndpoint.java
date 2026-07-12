package dev.rafex.ether.websocket.proxy.jetty12;

/*-
 * #%L
 * ether-websocket-proxy-jetty12
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

import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import dev.rafex.ether.websocket.core.WebSocketCloseStatus;
import dev.rafex.ether.websocket.core.WebSocketEndpoint;
import dev.rafex.ether.websocket.core.WebSocketSession;

public final class WebSocketProxyEndpoint implements WebSocketEndpoint {

    private final BackendResolver backendResolver;
    private final Duration connectTimeout;

    public WebSocketProxyEndpoint(final BackendResolver backendResolver) {
        this(backendResolver, Duration.ofSeconds(10));
    }

    public WebSocketProxyEndpoint(final BackendResolver backendResolver, final Duration connectTimeout) {
        this.backendResolver = Objects.requireNonNull(backendResolver, "backendResolver");
        this.connectTimeout = Objects.requireNonNull(connectTimeout, "connectTimeout");
    }

    @Override
    public void onOpen(final WebSocketSession clientSession) throws Exception {
        final var backendUri = backendResolver.resolve(clientSession);
        if (backendUri == null) {
            clientSession.close(WebSocketCloseStatus.SERVER_ERROR);
            return;
        }

        final var httpClient = new HttpClient();
        final var wsClient = new WebSocketClient(httpClient);
        httpClient.setConnectTimeout(connectTimeout.toMillis());
        httpClient.start();

        try {
            wsClient.start();
            final var backendListener = new BackendSessionListener(clientSession);
            final var backendFuture = wsClient.connect(backendListener, backendUri);
            backendFuture.get(connectTimeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);

            final var backendSession = backendListener.backendSession;
            if (backendSession == null) {
                clientSession.close(WebSocketCloseStatus.SERVER_ERROR);
                return;
            }

            final var bridge = new ProxyBridge(clientSession, backendSession, httpClient);
            clientSession.attribute("proxy-bridge", bridge);
        } catch (final Exception e) {
            clientSession.close(WebSocketCloseStatus.SERVER_ERROR);
            httpClient.stop();
        }
    }

    @Override
    public void onText(final WebSocketSession session, final String message) throws Exception {
        final var bridge = bridge(session);
        if (bridge != null) {
            bridge.backend.sendText(message, Callback.NOOP);
        }
    }

    @Override
    public void onBinary(final WebSocketSession session, final ByteBuffer message) throws Exception {
        final var bridge = bridge(session);
        if (bridge != null) {
            bridge.backend.sendBinary(message, Callback.NOOP);
        }
    }

    @Override
    public void onClose(final WebSocketSession session, final WebSocketCloseStatus closeStatus) throws Exception {
        final var bridge = bridge(session);
        if (bridge != null) {
            bridge.close(closeStatus);
        }
    }

    @Override
    public void onError(final WebSocketSession session, final Throwable error) {
        final var bridge = bridge(session);
        if (bridge != null) {
            bridge.close(WebSocketCloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public Set<String> subprotocols() {
        return Set.of();
    }

    private static ProxyBridge bridge(final WebSocketSession session) {
        return (ProxyBridge) session.attribute("proxy-bridge");
    }

    private static final class ProxyBridge {

        final Session backend;
        final HttpClient httpClient;

        ProxyBridge(final WebSocketSession client, final Session backend, final HttpClient httpClient) {
            this.backend = backend;
            this.httpClient = httpClient;
        }

        void close(final WebSocketCloseStatus status) {
            try {
                if (backend.isOpen()) {
                    backend.close(status.code(), status.reason(), Callback.NOOP);
                }
            } catch (final Exception ignored) {
            }
            try {
                httpClient.stop();
            } catch (final Exception ignored) {
            }
        }
    }

    private static final class BackendSessionListener implements Session.Listener.AutoDemanding {

        final WebSocketSession clientSession;
        volatile Session backendSession;

        BackendSessionListener(final WebSocketSession clientSession) {
            this.clientSession = clientSession;
        }

        @Override
        public void onWebSocketOpen(final Session session) {
            this.backendSession = session;
        }

        @Override
        public void onWebSocketText(final String message) {
            if (clientSession.isOpen()) {
                clientSession.sendText(message);
            }
        }

        @Override
        public void onWebSocketBinary(final ByteBuffer payload, final org.eclipse.jetty.websocket.api.Callback callback) {
            if (clientSession.isOpen()) {
                clientSession.sendBinary(payload);
            }
            callback.succeed();
        }

        @Override
        public void onWebSocketClose(final int statusCode, final String reason,
                final org.eclipse.jetty.websocket.api.Callback callback) {
            if (clientSession.isOpen()) {
                clientSession.close(WebSocketCloseStatus.of(statusCode, reason));
            }
            callback.succeed();
        }

        @Override
        public void onWebSocketError(final Throwable cause) {
            if (clientSession.isOpen()) {
                clientSession.close(WebSocketCloseStatus.SERVER_ERROR);
            }
        }
    }
}
