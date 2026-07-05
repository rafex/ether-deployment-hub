package dev.rafex.ether.http.jetty12.exchange;

/*-
 * #%L
 * ether-http-jetty12
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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import dev.rafex.ether.http.core.HttpExchange.EventStream;

/**
 * Jetty 12 {@link EventStream} implementation.
 *
 * <p>Jetty forbids issuing a new {@link Response#write} before the {@link Callback} of the
 * previous one completes, but {@link #send}/{@link #comment}/{@link #close} may be invoked
 * concurrently (e.g. from a message-bus callback thread and a heartbeat scheduler thread), so
 * every write — including the final empty/last one — goes through a single queue that is
 * pumped one entry at a time.
 */
final class JettyEventStream implements EventStream {

    private static final ByteBuffer EMPTY = ByteBuffer.allocate(0);

    private final Response response;
    private final Callback finalCallback;
    private final Deque<byte[]> queue = new ArrayDeque<>();

    private boolean writePending;
    private boolean closing;
    private boolean finished;
    private Runnable onClose;

    JettyEventStream(final Response response, final Callback finalCallback) {
        this.response = response;
        this.finalCallback = finalCallback;
    }

    @Override
    public void send(final String event, final String data) {
        final var sb = new StringBuilder();
        if (event != null && !event.isBlank()) {
            sb.append("event: ").append(event).append('\n');
        }
        final String safeData = data == null ? "" : data;
        for (final String line : safeData.split("\n", -1)) {
            sb.append("data: ").append(line).append('\n');
        }
        sb.append('\n');
        enqueue(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void comment(final String text) {
        final String safe = (text == null ? "" : text).replace("\n", " ");
        enqueue((": " + safe + "\n\n").getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public synchronized void onClose(final Runnable callback) {
        this.onClose = callback;
    }

    @Override
    public void close() {
        synchronized (this) {
            if (closing) {
                return;
            }
            closing = true;
        }
        pump();
    }

    private void enqueue(final byte[] bytes) {
        synchronized (this) {
            if (closing || finished) {
                return;
            }
            queue.addLast(bytes);
        }
        pump();
    }

    /** Drains the queue one write at a time; issues the final empty write once drained and closing. */
    private void pump() {
        final byte[] next;
        final boolean last;
        synchronized (this) {
            if (writePending || finished) {
                return;
            }
            if (!queue.isEmpty()) {
                next = queue.pollFirst();
                last = false;
            } else if (closing) {
                next = null;
                last = true;
            } else {
                return;
            }
            writePending = true;
        }
        response.write(last, next == null ? EMPTY : ByteBuffer.wrap(next), new Callback() {
            @Override
            public void succeeded() {
                if (last) {
                    complete(null);
                } else {
                    synchronized (JettyEventStream.this) {
                        writePending = false;
                    }
                    pump();
                }
            }

            @Override
            public void failed(final Throwable cause) {
                complete(cause);
            }
        });
    }

    private void complete(final Throwable cause) {
        final Runnable callback;
        synchronized (this) {
            if (finished) {
                return;
            }
            finished = true;
            closing = true;
            queue.clear();
            callback = onClose;
        }
        if (cause == null) {
            finalCallback.succeeded();
        } else {
            finalCallback.failed(cause);
        }
        if (callback != null) {
            callback.run();
        }
    }
}
