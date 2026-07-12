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

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("WebSocket Core Contracts")
class WebSocketCoreTest {

    @Nested
    @DisplayName("WebSocketPatterns.match")
    class PatternsTest {

        @Test
        @DisplayName("exact match returns empty params")
        void exactMatch() {
            var result = WebSocketPatterns.match("/ws/echo", "/ws/echo");
            assertTrue(result.isPresent());
            assertTrue(result.get().isEmpty());
        }

        @Test
        @DisplayName("single path variable")
        void singleVariable() {
            var result = WebSocketPatterns.match("/chat/{room}", "/chat/lobby");
            assertTrue(result.isPresent());
            assertEquals(Map.of("room", "lobby"), result.get());
        }

        @Test
        @DisplayName("multiple path variables")
        void multipleVariables() {
            var result = WebSocketPatterns.match("/api/{version}/users/{id}", "/api/v2/users/42");
            assertTrue(result.isPresent());
            assertEquals(Map.of("version", "v2", "id", "42"), result.get());
        }

        @Test
        @DisplayName("wildcard /** matches any path")
        void wildcardMatch() {
            var result = WebSocketPatterns.match("/**", "/anything/goes/here");
            assertTrue(result.isPresent());
            assertTrue(result.get().isEmpty());
        }

        @Test
        @DisplayName("wildcard /** matches root")
        void wildcardRoot() {
            var result = WebSocketPatterns.match("/**", "/");
            assertTrue(result.isPresent());
        }

        @Test
        @DisplayName("segment count mismatch returns empty")
        void segmentCountMismatch() {
            var result = WebSocketPatterns.match("/ws/echo", "/ws/echo/extra");
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("literal mismatch returns empty")
        void literalMismatch() {
            var result = WebSocketPatterns.match("/ws/echo", "/ws/ping");
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("null pattern returns empty")
        void nullPattern() {
            var result = WebSocketPatterns.match(null, "/path");
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("blank pattern returns empty")
        void blankPattern() {
            var result = WebSocketPatterns.match("  ", "/path");
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("null path returns empty")
        void nullPath() {
            var result = WebSocketPatterns.match("/pattern", null);
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("path with trailing slash matches (split discards trailing empty)")
        void trailingSlash() {
            var result = WebSocketPatterns.match("/chat/{room}", "/chat/lobby/");
            assertTrue(result.isPresent());
            assertEquals(Map.of("room", "lobby"), result.get());
        }

        @Test
        @DisplayName("empty segment in path")
        void emptySegment() {
            var result = WebSocketPatterns.match("/a/{b}/c", "/a//c");
            assertTrue(result.isPresent());
            assertEquals(Map.of("b", ""), result.get());
        }
    }

    @Nested
    @DisplayName("WebSocketRouteMatcher")
    class RouteMatcherTest {

        private final List<WebSocketRoute> routes = List.of(
                WebSocketRoute.of("/ws/echo", new TestEndpoint()),
                WebSocketRoute.of("/chat/{room}", new TestEndpoint()));

        @Test
        @DisplayName("first match in route list")
        void firstMatch() {
            var result = WebSocketRouteMatcher.match("/ws/echo", routes);
            assertTrue(result.isPresent());
            assertEquals("/ws/echo", result.get().route().pattern());
            assertTrue(result.get().pathParams().isEmpty());
        }

        @Test
        @DisplayName("variable match extracts params")
        void variableMatch() {
            var result = WebSocketRouteMatcher.match("/chat/lobby", routes);
            assertTrue(result.isPresent());
            assertEquals(Map.of("room", "lobby"), result.get().pathParams());
        }

        @Test
        @DisplayName("no match returns empty")
        void noMatch() {
            var result = WebSocketRouteMatcher.match("/unknown", routes);
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("null routes returns empty")
        void nullRoutes() {
            var result = WebSocketRouteMatcher.match("/path", null);
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("empty routes returns empty")
        void emptyRoutes() {
            var result = WebSocketRouteMatcher.match("/path", List.of());
            assertFalse(result.isPresent());
        }
    }

    @Nested
    @DisplayName("WebSocketCloseStatus")
    class CloseStatusTest {

        @Test
        @DisplayName("constants have correct codes")
        void constantCodes() {
            assertEquals(1000, WebSocketCloseStatus.NORMAL.code());
            assertEquals(1001, WebSocketCloseStatus.GOING_AWAY.code());
            assertEquals(1002, WebSocketCloseStatus.PROTOCOL_ERROR.code());
            assertEquals(1003, WebSocketCloseStatus.NOT_ACCEPTABLE.code());
            assertEquals(1011, WebSocketCloseStatus.SERVER_ERROR.code());
        }

        @Test
        @DisplayName("null reason normalized to empty string")
        void nullReason() {
            var status = WebSocketCloseStatus.of(4000, null);
            assertEquals("", status.reason());
        }

        @Test
        @DisplayName("factory preserves code and reason")
        void factoryCreation() {
            var status = WebSocketCloseStatus.of(4001, "custom_reason");
            assertEquals(4001, status.code());
            assertEquals("custom_reason", status.reason());
        }
    }

    @Nested
    @DisplayName("WebSocketRoute")
    class RouteTest {

        @Test
        @DisplayName("of() creates valid route")
        void factoryCreatesRoute() {
            var endpoint = new TestEndpoint();
            var route = WebSocketRoute.of("/path", endpoint);
            assertEquals("/path", route.pattern());
            assertSame(endpoint, route.endpoint());
        }

        @Test
        @DisplayName("null pattern throws")
        void nullPatternThrows() {
            assertThrows(NullPointerException.class, () -> WebSocketRoute.of(null, new TestEndpoint()));
        }

        @Test
        @DisplayName("null endpoint throws")
        void nullEndpointThrows() {
            assertThrows(NullPointerException.class, () -> WebSocketRoute.of("/path", null));
        }
    }

    private static final class TestEndpoint implements WebSocketEndpoint {
    }
}
