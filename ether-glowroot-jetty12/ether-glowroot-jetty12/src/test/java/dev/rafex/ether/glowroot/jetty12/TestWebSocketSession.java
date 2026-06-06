package dev.rafex.ether.glowroot.jetty12;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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
import dev.rafex.ether.websocket.core.WebSocketSession;

/**
 * In-memory {@link WebSocketSession} test double.
 *
 * <p>
 * Tracks sent messages and stored attributes so assertions can verify what the
 * endpoint wrote to the session.
 * </p>
 */
final class TestWebSocketSession implements WebSocketSession {

    private final String id;
    private final String path;
    private final Map<String, Object> attributes = new HashMap<>();
    private boolean open = true;

    TestWebSocketSession(final String path) {
        this.id = UUID.randomUUID().toString();
        this.path = path;
    }

    /* ── WebSocketSession implementation ── */

    @Override
    public String id() {
        return id;
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public String subprotocol() {
        return null;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public String pathParam(final String name) {
        return null;
    }

    @Override
    public String queryFirst(final String name) {
        return null;
    }

    @Override
    public List<String> queryAll(final String name) {
        return List.of();
    }

    @Override
    public String headerFirst(final String name) {
        return null;
    }

    @Override
    public Object attribute(final String name) {
        return attributes.get(name);
    }

    @Override
    public void attribute(final String name, final Object value) {
        attributes.put(name, value);
    }

    @Override
    public Map<String, String> pathParams() {
        return Map.of();
    }

    @Override
    public Map<String, List<String>> queryParams() {
        return Map.of();
    }

    @Override
    public Map<String, List<String>> headers() {
        return Map.of();
    }

    @Override
    public CompletionStage<Void> sendText(final String text) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> sendBinary(final ByteBuffer data) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> close(final WebSocketCloseStatus status) {
        this.open = false;
        return CompletableFuture.completedFuture(null);
    }
}
