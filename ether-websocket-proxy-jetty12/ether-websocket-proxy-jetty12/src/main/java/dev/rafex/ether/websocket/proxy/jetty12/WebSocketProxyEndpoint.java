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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import dev.rafex.ether.websocket.core.WebSocketCloseStatus;
import dev.rafex.ether.websocket.core.WebSocketEndpoint;
import dev.rafex.ether.websocket.core.WebSocketSession;

public final class WebSocketProxyEndpoint implements WebSocketEndpoint {

    private static final Logger LOG = Logger.getLogger(WebSocketProxyEndpoint.class.getName());

    private final BackendResolver backendResolver;
    private final Duration connectTimeout;
    private final WebSocketClient wsClient;
    private final ConcurrentMap<String, Session> backendSessions = new ConcurrentHashMap<>();

    public WebSocketProxyEndpoint(final BackendResolver backendResolver, final WebSocketClient wsClient) {
        this(backendResolver, wsClient, Duration.ofSeconds(10));
    }

    public WebSocketProxyEndpoint(final BackendResolver backendResolver, final WebSocketClient wsClient,
            final Duration connectTimeout) {
        this.backendResolver = Objects.requireNonNull(backendResolver, "backendResolver");
        this.wsClient = Objects.requireNonNull(wsClient, "wsClient");
        this.connectTimeout = Objects.requireNonNull(connectTimeout, "connectTimeout");
    }

    public int activeConnections() {
        return backendSessions.size();
    }

    @Override
    public void onOpen(final WebSocketSession clientSession) throws Exception {
        final var clientId = clientSession.id();
        final var requestId = requestIdOf(clientSession);
        final var backendUri = backendResolver.resolve(clientSession);
        if (backendUri == null) {
            LOG.warning(() -> "[%s] proxy: client=%s backend URI null".formatted(requestId, clientId));
            clientSession.close(WebSocketCloseStatus.SERVER_ERROR);
            return;
        }

        final var start = System.nanoTime();
        try {
            final var backendListener = new BackendSessionListener(clientSession, requestId);
            final var backendSession = wsClient.connect(backendListener, backendUri)
                    .get(connectTimeout.toMillis(), TimeUnit.MILLISECONDS);

            final var previous = backendSessions.put(clientId, backendSession);
            if (previous != null) {
                previous.close(WebSocketCloseStatus.GOING_AWAY.code(),
                        WebSocketCloseStatus.GOING_AWAY.reason(), Callback.NOOP);
            }

            final var elapsed = Duration.ofNanos(System.nanoTime() - start);
            LOG.info(() -> "[%s] proxy: client=%s → backend=%s connected (%dms, total=%d)"
                    .formatted(requestId, clientId, backendUri, elapsed.toMillis(), activeConnections()));
        } catch (final Exception e) {
            final var elapsed = Duration.ofNanos(System.nanoTime() - start);
            LOG.log(Level.WARNING, e,
                    () -> "[%s] proxy: client=%s → backend=%s failed (%dms, total=%d)"
                            .formatted(requestId, clientId, backendUri, elapsed.toMillis(), activeConnections()));
            clientSession.close(WebSocketCloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void onText(final WebSocketSession session, final String message) throws Exception {
        final var backend = backendSessions.get(session.id());
        if (backend != null && backend.isOpen()) {
            LOG.fine(() -> "[%s] proxy: client=%s → backend TEXT(%d)"
                    .formatted(requestIdOf(session), session.id(), message.length()));
            backend.sendText(message, Callback.NOOP);
            return;
        }
        LOG.warning(() -> "[%s] proxy: client=%s onText but backend unavailable"
                .formatted(requestIdOf(session), session.id()));
    }

    @Override
    public void onBinary(final WebSocketSession session, final ByteBuffer message) throws Exception {
        final var backend = backendSessions.get(session.id());
        if (backend != null && backend.isOpen()) {
            final var size = message == null ? 0 : message.remaining();
            LOG.fine(() -> "[%s] proxy: client=%s → backend BIN(%d)"
                    .formatted(requestIdOf(session), session.id(), size));
            backend.sendBinary(message, Callback.NOOP);
            return;
        }
        LOG.warning(() -> "[%s] proxy: client=%s onBinary but backend unavailable"
                .formatted(requestIdOf(session), session.id()));
    }

    @Override
    public void onClose(final WebSocketSession session, final WebSocketCloseStatus closeStatus) throws Exception {
        final var clientId = session.id();
        final var requestId = requestIdOf(session);
        closeBackend(clientId, closeStatus);
        LOG.info(() -> "[%s] proxy: client=%s closed (%d %s, total=%d)"
                .formatted(requestId, clientId, closeStatus.code(), closeStatus.reason(), activeConnections()));
    }

    @Override
    public void onError(final WebSocketSession session, final Throwable error) {
        final var clientId = session.id();
        final var requestId = requestIdOf(session);
        LOG.log(Level.WARNING, error,
                () -> "[%s] proxy: client=%s error (total=%d)".formatted(requestId, clientId, activeConnections()));
        closeBackend(clientId, WebSocketCloseStatus.SERVER_ERROR);
    }

    @Override
    public Set<String> subprotocols() {
        return Set.of();
    }

    private void closeBackend(final String clientId, final WebSocketCloseStatus status) {
        final var backend = backendSessions.remove(clientId);
        if (backend != null && backend.isOpen()) {
            try {
                backend.close(status.code(), status.reason(), Callback.NOOP);
            } catch (final Exception e) {
                LOG.log(Level.FINE, e, () -> "proxy: client=%s backend close error".formatted(clientId));
            }
        }
    }

    private static String requestIdOf(final WebSocketSession session) {
        final var attr = session.attribute("requestId");
        return attr != null ? attr.toString() : session.id();
    }

    private static final class BackendSessionListener implements Session.Listener.AutoDemanding {

        private final WebSocketSession clientSession;
        private final String requestId;

        BackendSessionListener(final WebSocketSession clientSession, final String requestId) {
            this.clientSession = clientSession;
            this.requestId = requestId;
        }

        @Override
        public void onWebSocketOpen(final Session session) {
            LOG.fine(() -> "[%s] proxy: backend session opened for client=%s"
                    .formatted(requestId, clientSession.id()));
        }

        @Override
        public void onWebSocketText(final String message) {
            if (clientSession.isOpen()) {
                LOG.fine(() -> "[%s] proxy: backend → client=%s TEXT(%d)"
                        .formatted(requestId, clientSession.id(), message.length()));
                clientSession.sendText(message);
                return;
            }
            LOG.warning(() -> "[%s] proxy: backend text but client=%s closed"
                    .formatted(requestId, clientSession.id()));
        }

        @Override
        public void onWebSocketBinary(final ByteBuffer payload, final Callback callback) {
            if (clientSession.isOpen()) {
                final var size = payload == null ? 0 : payload.remaining();
                LOG.fine(() -> "[%s] proxy: backend → client=%s BIN(%d)"
                        .formatted(requestId, clientSession.id(), size));
                clientSession.sendBinary(payload);
            }
            callback.succeed();
        }

        @Override
        public void onWebSocketClose(final int statusCode, final String reason, final Callback callback) {
            LOG.info(() -> "[%s] proxy: backend closed (%d %s) for client=%s"
                    .formatted(requestId, statusCode, reason, clientSession.id()));
            if (clientSession.isOpen()) {
                clientSession.close(WebSocketCloseStatus.of(statusCode, reason));
            }
            callback.succeed();
        }

        @Override
        public void onWebSocketError(final Throwable cause) {
            LOG.log(Level.WARNING, cause,
                    () -> "[%s] proxy: backend error for client=%s".formatted(requestId, clientSession.id()));
            if (clientSession.isOpen()) {
                clientSession.close(WebSocketCloseStatus.SERVER_ERROR);
            }
        }
    }
}
