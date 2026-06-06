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

import java.util.List;
import java.util.Optional;

/**
 * Utilidad que resuelve la primera {@link WebSocketRoute} que coincide con un path dado.
 * Recorre la lista de rutas en orden y retorna la primera coincidencia encontrada.
 */
public final class WebSocketRouteMatcher {

    private WebSocketRouteMatcher() {
    }

    /**
     * Busca la primera ruta de la lista cuyo patrón coincida con el path especificado.
     *
     * @param path   path entrante de la conexión WebSocket
     * @param routes lista de rutas registradas; se evalúan en orden de inserción
     * @return un {@link Optional} con el {@link WebSocketRouteMatch} si se encontró coincidencia;
     *         {@link Optional#empty()} si la lista es nula, vacía o ninguna ruta coincide
     */
    public static Optional<WebSocketRouteMatch> match(final String path, final List<WebSocketRoute> routes) {
        if (routes == null || routes.isEmpty()) {
            return Optional.empty();
        }
        for (final var route : routes) {
            final var match = WebSocketPatterns.match(route.pattern(), path);
            if (match.isPresent()) {
                return Optional.of(new WebSocketRouteMatch(route, match.get()));
            }
        }
        return Optional.empty();
    }
}
