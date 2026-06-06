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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Utilidad para el matching de patrones de rutas WebSocket.
 * Soporta segmentos con variables de captura entre llaves (por ejemplo, {@code /chat/{room}})
 * y el comodín global {@code /**} que coincide con cualquier path.
 */
public final class WebSocketPatterns {

    private WebSocketPatterns() {
    }

    /**
     * Comprueba si el path dado coincide con el patrón de ruta especificado.
     * Si coinciden, devuelve los parámetros de path extraídos como un mapa.
     *
     * @param pattern patrón de ruta con segmentos literales o variables {@code {nombre}}
     * @param path    path entrante a comparar contra el patrón
     * @return un {@link Optional} con el mapa de parámetros extraídos si hay coincidencia;
     *         {@link Optional#empty()} si no coinciden o si los argumentos son inválidos
     */
    public static Optional<Map<String, String>> match(final String pattern, final String path) {
        if (pattern == null || pattern.isBlank() || path == null || path.isBlank()) {
            return Optional.empty();
        }
        if ("/**".equals(pattern)) {
            return Optional.of(Map.of());
        }

        final var patternSegments = split(pattern);
        final var pathSegments = split(path);
        if (patternSegments.size() != pathSegments.size()) {
            return Optional.empty();
        }

        final var params = new LinkedHashMap<String, String>();
        for (int i = 0; i < patternSegments.size(); i++) {
            final var expected = patternSegments.get(i);
            final var actual = pathSegments.get(i);
            if (expected.startsWith("{") && expected.endsWith("}")) {
                params.put(expected.substring(1, expected.length() - 1), actual);
                continue;
            }
            if (!expected.equals(actual)) {
                return Optional.empty();
            }
        }

        return Optional.of(params);
    }

    /**
     * Divide un path en sus segmentos, eliminando la barra inicial si existe.
     *
     * @param path path a dividir (por ejemplo, {@code "/chat/room1"})
     * @return lista de segmentos no vacíos; lista vacía si el path es raíz o vacío
     */
    private static List<String> split(final String path) {
        final var cleaned = path.startsWith("/") ? path.substring(1) : path;
        if (cleaned.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(cleaned.split("/"));
    }
}
