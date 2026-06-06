package dev.rafex.ether.glowroot.jetty12;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

@DisplayName("GlowrootWebSocketEndpointWrapper")
class GlowrootWebSocketEndpointWrapperTest {

    /* ── constructor guard ── */

    @Test
    @DisplayName("null delegate throws IllegalArgumentException")
    void nullDelegateThrows() {
        assertThrows(IllegalArgumentException.class, () -> new GlowrootWebSocketEndpointWrapper(null));
    }

    /* ── onOpen ── */

    @Test
    @DisplayName("onOpen delegates to the wrapped endpoint")
    void onOpenDelegates() throws Exception {
        final var called = new AtomicBoolean(false);
        final var delegate = new SpyEndpoint() {
            @Override
            public void onOpen(final WebSocketSession s) {
                called.set(true);
            }
        };
        final var wrapper = new GlowrootWebSocketEndpointWrapper(delegate);

        wrapper.onOpen(session("/ws/chat"));
        assertTrue(called.get());
    }

    /* ── onText ── */

    @Test
    @DisplayName("onText delegates message to the wrapped endpoint")
    void onTextDelegates() throws Exception {
        final var received = new AtomicReference<String>();
        final var delegate = new SpyEndpoint() {
            @Override
            public void onText(final WebSocketSession s, final String msg) {
                received.set(msg);
            }
        };
        final var wrapper = new GlowrootWebSocketEndpointWrapper(delegate);

        wrapper.onText(session("/ws/chat"), "hello");
        assertEquals("hello", received.get());
    }

    @Test
    @DisplayName("onText with null message does not throw")
    void onTextNullMessage() throws Exception {
        final var wrapper = wrapEmpty();
        assertDoesNotThrow(() -> wrapper.onText(session("/ws/chat"), null));
    }

    /* ── onBinary ── */

    @Test
    @DisplayName("onBinary delegates buffer to the wrapped endpoint")
    void onBinaryDelegates() throws Exception {
        final var received = new AtomicReference<ByteBuffer>();
        final var delegate = new SpyEndpoint() {
            @Override
            public void onBinary(final WebSocketSession s, final ByteBuffer msg) {
                received.set(msg);
            }
        };
        final var wrapper = new GlowrootWebSocketEndpointWrapper(delegate);
        final var buf = ByteBuffer.wrap(new byte[] { 1, 2, 3 });

        wrapper.onBinary(session("/ws/stream"), buf);
        assertSame(buf, received.get());
    }

    @Test
    @DisplayName("onBinary with null buffer does not throw")
    void onBinaryNullBuffer() throws Exception {
        final var wrapper = wrapEmpty();
        assertDoesNotThrow(() -> wrapper.onBinary(session("/ws/stream"), null));
    }

    /* ── onClose ── */

    @Test
    @DisplayName("onClose delegates close status to the wrapped endpoint")
    void onCloseDelegates() throws Exception {
        final var received = new AtomicReference<WebSocketCloseStatus>();
        final var delegate = new SpyEndpoint() {
            @Override
            public void onClose(final WebSocketSession s, final WebSocketCloseStatus status) {
                received.set(status);
            }
        };
        final var wrapper = new GlowrootWebSocketEndpointWrapper(delegate);

        wrapper.onClose(session("/ws/chat"), WebSocketCloseStatus.NORMAL);
        assertEquals(WebSocketCloseStatus.NORMAL, received.get());
    }

    @Test
    @DisplayName("onClose with null status does not throw")
    void onCloseNullStatus() throws Exception {
        final var wrapper = wrapEmpty();
        assertDoesNotThrow(() -> wrapper.onClose(session("/ws/chat"), null));
    }

    /* ── onError ── */

    @Test
    @DisplayName("onError delegates throwable to the wrapped endpoint")
    void onErrorDelegates() {
        final var received = new AtomicReference<Throwable>();
        final var delegate = new SpyEndpoint() {
            @Override
            public void onError(final WebSocketSession s, final Throwable t) {
                received.set(t);
            }
        };
        final var wrapper = new GlowrootWebSocketEndpointWrapper(delegate);
        final var error = new RuntimeException("ws-error");

        wrapper.onError(session("/ws/chat"), error);
        assertSame(error, received.get());
    }

    @Test
    @DisplayName("onError with null throwable does not throw")
    void onErrorNullThrowable() {
        final var wrapper = wrapEmpty();
        assertDoesNotThrow(() -> wrapper.onError(session("/ws/chat"), null));
    }

    /* ── subprotocols ── */

    @Test
    @DisplayName("subprotocols() delegates to the wrapped endpoint")
    void subprotocolsDelegates() {
        final var delegate = new SpyEndpoint() {
            @Override
            public Set<String> subprotocols() {
                return Set.of("v1.chat");
            }
        };
        final var wrapper = new GlowrootWebSocketEndpointWrapper(delegate);

        assertEquals(Set.of("v1.chat"), wrapper.subprotocols());
    }

    /* ── exception propagation ── */

    @Test
    @DisplayName("exception thrown by delegate.onOpen propagates")
    void onOpenExceptionPropagates() {
        final var delegate = new SpyEndpoint() {
            @Override
            public void onOpen(final WebSocketSession s) throws Exception {
                throw new Exception("open-fail");
            }
        };
        final var wrapper = new GlowrootWebSocketEndpointWrapper(delegate);

        assertThrows(Exception.class, () -> wrapper.onOpen(session("/ws/chat")));
    }

    @Test
    @DisplayName("exception thrown by delegate.onText propagates")
    void onTextExceptionPropagates() {
        final var delegate = new SpyEndpoint() {
            @Override
            public void onText(final WebSocketSession s, final String msg) throws Exception {
                throw new Exception("text-fail");
            }
        };
        final var wrapper = new GlowrootWebSocketEndpointWrapper(delegate);

        assertThrows(Exception.class, () -> wrapper.onText(session("/ws/chat"), "msg"));
    }

    /* ── helpers ── */

    private static TestWebSocketSession session(final String path) {
        return new TestWebSocketSession(path);
    }

    private static GlowrootWebSocketEndpointWrapper wrapEmpty() {
        return new GlowrootWebSocketEndpointWrapper(new SpyEndpoint());
    }

    private static GlowrootWebSocketEndpointWrapper wrapSpy(
            final java.util.function.Consumer<SpyEndpoint> configurator) {
        final var spy = new SpyEndpoint();
        configurator.accept(spy);
        return new GlowrootWebSocketEndpointWrapper(spy);
    }

    /** Minimal {@link WebSocketEndpoint} that does nothing by default. */
    static class SpyEndpoint implements WebSocketEndpoint {
        boolean onOpenCalled = false;

        @Override
        public void onOpen(final WebSocketSession session) throws Exception {
            onOpenCalled = true;
        }
    }
}
