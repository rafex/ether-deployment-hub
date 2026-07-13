package dev.rafex.ether.websocket.jetty12;

/*-
 * #%L
 * ether-websocket-jetty12
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;
import org.eclipse.jetty.websocket.server.ServerUpgradeRequest;
import org.eclipse.jetty.websocket.server.ServerUpgradeResponse;

import dev.rafex.ether.websocket.core.WebSocketPatterns;
import dev.rafex.ether.websocket.core.WebSocketRoute;

/**
 * WebSocket upgrade helpers shared by the Jetty server factories.
 *
 * <p>Both {@link JettyWebSocketServerFactory} and the WebSocket routing in
 * {@code ether-http-jetty12}'s {@code JettyServerFactory} build the same
 * endpoint adapter from an upgrade request: extracting path params, headers and
 * query params, and negotiating a subprotocol. This class centralises that
 * logic so it exists once.
 */
public final class JettyWebSocketUpgrades {

    private JettyWebSocketUpgrades() {
    }

    /**
     * Builds the {@link JettyWebSocketEndpointAdapter} for a matched route from
     * an upgrade request, negotiating a subprotocol on the response if supported.
     *
     * @param route    the matched WebSocket route
     * @param request  the upgrade request
     * @param response the upgrade response (its accepted subprotocol may be set)
     * @return the endpoint adapter instance
     */
    public static Object createEndpoint(final WebSocketRoute route, final ServerUpgradeRequest request,
            final ServerUpgradeResponse response) {
        final var path = request.getHttpURI().getPath();
        final var pathParams = WebSocketPatterns.match(route.pattern(), path).orElse(Map.of());
        final var headers = headersOf(request);
        final var queryParams = queryOf(request);
        negotiateSubprotocol(route, request, response);
        return new JettyWebSocketEndpointAdapter(route.endpoint(), path, pathParams, queryParams, headers);
    }

    /**
     * Negotiates a subprotocol if the endpoint supports any. Picks the first
     * requested subprotocol that the endpoint declares as supported.
     */
    private static void negotiateSubprotocol(final WebSocketRoute route, final ServerUpgradeRequest request,
            final ServerUpgradeResponse response) {
        final var supported = route.endpoint().subprotocols();
        if (supported == null || supported.isEmpty()) {
            return;
        }
        for (final var requested : request.getSubProtocols()) {
            if (supported.contains(requested)) {
                response.setAcceptedSubProtocol(requested);
                return;
            }
        }
    }

    /** Extracts HTTP headers from the upgrade request into an unmodifiable map. */
    private static Map<String, List<String>> headersOf(final ServerUpgradeRequest request) {
        final var out = new LinkedHashMap<String, List<String>>();
        for (final var field : request.getHeaders()) {
            out.computeIfAbsent(field.getName(), ignored -> new ArrayList<>()).add(field.getValue());
        }
        return copyMultiMap(out);
    }

    /** Decodes the query string from the upgrade request into an unmodifiable map. */
    private static Map<String, List<String>> queryOf(final ServerUpgradeRequest request) {
        final MultiMap<String> params = new MultiMap<>();
        final var rawQuery = request.getHttpURI().getQuery();
        if (rawQuery != null && !rawQuery.isEmpty()) {
            UrlEncoded.decodeTo(rawQuery, params, StandardCharsets.UTF_8);
        }

        final var out = new LinkedHashMap<String, List<String>>();
        for (final var key : params.keySet()) {
            final var values = params.getValues(key);
            out.put(key, values == null ? List.of() : List.copyOf(values));
        }
        return Map.copyOf(out);
    }

    /** Returns an unmodifiable deep copy of the given multi-valued map. */
    private static Map<String, List<String>> copyMultiMap(final Map<String, List<String>> input) {
        final var out = new LinkedHashMap<String, List<String>>();
        for (final var entry : input.entrySet()) {
            out.put(entry.getKey(), entry.getValue() == null ? List.of() : List.copyOf(entry.getValue()));
        }
        return Map.copyOf(out);
    }
}
