package dev.rafex.ether.glowroot.jetty12;

import java.nio.ByteBuffer;
import java.util.Set;

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

import dev.rafex.ether.websocket.core.WebSocketCloseStatus;
import dev.rafex.ether.websocket.core.WebSocketEndpoint;
import dev.rafex.ether.websocket.core.WebSocketSession;

/**
 * Decorator that instruments a {@link WebSocketEndpoint} with Glowroot APM.
 *
 * <p>
 * Wraps every WebSocket lifecycle event (open, text, binary, close, error) and
 * records it as a Glowroot {@code "WebSocket"} transaction, tagging each one
 * with the session id, path, and event-specific metadata.
 * </p>
 *
 * <p>
 * Usage — wrap the endpoint before registering it:
 * </p>
 * 
 * <pre>{@code
 * routeRegistry.add(WebSocketRoute.of("/ws/chat", new GlowrootWebSocketEndpointWrapper(new ChatEndpoint())));
 * }</pre>
 */
public final class GlowrootWebSocketEndpointWrapper implements WebSocketEndpoint {

    private final WebSocketEndpoint delegate;

    public GlowrootWebSocketEndpointWrapper(final WebSocketEndpoint delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate must not be null");
        }
        this.delegate = delegate;
    }

    @Override
    public void onOpen(final WebSocketSession session) throws Exception {
        tag(session, "OPEN");
        try {
            Glowroot.addTransactionAttribute("websocket.event", "open");
        } catch (final Throwable ignore) {
        }
        delegate.onOpen(session);
    }

    @Override
    public void onText(final WebSocketSession session, final String message) throws Exception {
        tag(session, "TEXT");
        try {
            Glowroot.addTransactionAttribute("websocket.event", "text");
            Glowroot.addTransactionAttribute("websocket.message_length",
                    String.valueOf(message == null ? 0 : message.length()));
        } catch (final Throwable ignore) {
        }
        delegate.onText(session, message);
    }

    @Override
    public void onBinary(final WebSocketSession session, final ByteBuffer message) throws Exception {
        tag(session, "BINARY");
        try {
            Glowroot.addTransactionAttribute("websocket.event", "binary");
            Glowroot.addTransactionAttribute("websocket.message_length",
                    String.valueOf(message == null ? 0 : message.remaining()));
        } catch (final Throwable ignore) {
        }
        delegate.onBinary(session, message);
    }

    @Override
    public void onClose(final WebSocketSession session, final WebSocketCloseStatus closeStatus) throws Exception {
        tag(session, "CLOSE");
        try {
            Glowroot.addTransactionAttribute("websocket.event", "close");
            if (closeStatus != null) {
                Glowroot.addTransactionAttribute("websocket.close_code", String.valueOf(closeStatus.code()));
                Glowroot.addTransactionAttribute("websocket.close_reason", closeStatus.reason());
            }
        } catch (final Throwable ignore) {
        }
        delegate.onClose(session, closeStatus);
    }

    @Override
    public void onError(final WebSocketSession session, final Throwable error) {
        tag(session, "ERROR");
        try {
            Glowroot.addTransactionAttribute("websocket.event", "error");
            if (error != null) {
                Glowroot.addTransactionAttribute("error", error.getClass().getName());
                Glowroot.addTransactionAttribute("error.message", error.getMessage() == null ? "" : error.getMessage());
            }
        } catch (final Throwable ignore) {
        }
        delegate.onError(session, error);
    }

    @Override
    public Set<String> subprotocols() {
        return delegate.subprotocols();
    }

    private static void tag(final WebSocketSession session, final String event) {
        try {
            Glowroot.setTransactionType("WebSocket");
            Glowroot.setTransactionName(event + " " + session.path());
            Glowroot.addTransactionAttribute("websocket.session_id", session.id());
            Glowroot.addTransactionAttribute("websocket.path", session.path());
        } catch (final Throwable ignore) {
            // Glowroot agent not present; do not affect WebSocket handling
        }
    }
}
