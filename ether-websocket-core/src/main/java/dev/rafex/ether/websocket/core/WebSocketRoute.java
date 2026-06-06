package dev.rafex.ether.websocket.core;

/*-
 * #%L
 * ether-websocket-core
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

import java.util.Objects;

/**
 * Vincula un patrón de ruta WebSocket con el {@link WebSocketEndpoint} que la maneja.
 * El patrón define qué URLs entrantes activan el endpoint asociado.
 */
public record WebSocketRoute(String pattern, WebSocketEndpoint endpoint) {

    public WebSocketRoute {
        Objects.requireNonNull(pattern, "pattern");
        Objects.requireNonNull(endpoint, "endpoint");
    }

    /**
     * Crea una ruta WebSocket que asocia el patrón dado con el endpoint indicado.
     *
     * @param pattern  patrón de ruta a comparar contra paths entrantes (por ejemplo, {@code "/chat"})
     * @param endpoint endpoint que procesa las conexiones que coinciden con el patrón
     * @return nueva instancia de {@code WebSocketRoute}
     * @throws NullPointerException si {@code pattern} o {@code endpoint} son {@code null}
     */
    public static WebSocketRoute of(final String pattern, final WebSocketEndpoint endpoint) {
        return new WebSocketRoute(pattern, endpoint);
    }
}
