package dev.rafex.ether.http.core;

/*-
 * #%L
 * ether-http-core
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

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface HttpExchange {

    String method();

    String path();

    String pathParam(String name);

    String queryFirst(String name);

    List<String> queryAll(String name);

    Map<String, String> pathParams();

    Map<String, List<String>> queryParams();

    Set<String> allowedMethods();

    void json(int status, Object body);

    void text(int status, String body);

    void noContent(int status);

    default void methodNotAllowed() {
        json(405, Map.of("error", "method_not_allowed"));
    }

    default void options() {
        noContent(204);
    }

    /**
     * Starts a Server-Sent Events stream. Sets the transport's event-stream headers and
     * returns an {@link EventStream} for subsequent writes; {@link #json}, {@link #text} and
     * {@link #noContent} must not be called after this.
     *
     * <p>Default throws — only transports that implement long-lived/chunked writes (e.g. the
     * Jetty 12 transport) support this.
     */
    default EventStream startEventStream() {
        throw new UnsupportedOperationException("SSE not supported by this transport");
    }

    /** A long-lived, transport-agnostic handle for pushing Server-Sent Events. */
    interface EventStream {

        /** Sends one SSE event. {@code event} may be null for an unnamed "message" event. */
        void send(String event, String data);

        /** Sends an SSE comment line (e.g. for heartbeats); ignored by EventSource clients. */
        void comment(String text);

        /** Registers a callback invoked once when the stream ends (client disconnect or {@link #close()}). */
        void onClose(Runnable callback);

        /** Ends the stream. Safe to call more than once. */
        void close();
    }
}
