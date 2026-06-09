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

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * Define el contrato de un punto final WebSocket. Las implementaciones reciben
 * notificaciones del ciclo de vida de la conexión: apertura, mensajes de texto/binario,
 * cierre y errores. Todos los métodos tienen una implementación vacía por defecto para
 * que las subclases solo sobreescriban los eventos que necesiten.
 */
public interface WebSocketEndpoint {

    /**
     * Invocado cuando la conexión WebSocket se abre exitosamente.
     *
     * @param session sesión WebSocket activa
     * @throws Exception si ocurre un error durante el procesamiento
     */
    default void onOpen(final WebSocketSession session) throws Exception {
    }

    /**
     * Invocado cuando se recibe un mensaje de texto del cliente.
     *
     * @param session sesión WebSocket activa
     * @param message contenido de texto recibido
     * @throws Exception si ocurre un error durante el procesamiento
     */
    default void onText(final WebSocketSession session, final String message) throws Exception {
    }

    /**
     * Invocado cuando se recibe un mensaje binario del cliente.
     *
     * @param session sesión WebSocket activa
     * @param message contenido binario recibido
     * @throws Exception si ocurre un error durante el procesamiento
     */
    default void onBinary(final WebSocketSession session, final ByteBuffer message) throws Exception {
    }

    /**
     * Invocado cuando el cliente solicita cerrar la conexión.
     *
     * @param session     sesión WebSocket activa
     * @param closeStatus estado de cierre indicado por el cliente
     * @throws Exception si ocurre un error durante el procesamiento
     */
    default void onClose(final WebSocketSession session, final WebSocketCloseStatus closeStatus) throws Exception {
    }

    /**
     * Invocado cuando ocurre un error en la conexión WebSocket.
     *
     * @param session sesión WebSocket activa
     * @param error   excepción que causó el error
     */
    default void onError(final WebSocketSession session, final Throwable error) {
    }

    /**
     * Devuelve el conjunto de subprotocolos WebSocket que este endpoint acepta.
     * El servidor seleccionará un subprotocolo de esta lista durante el handshake.
     *
     * @return conjunto de nombres de subprotocolo soportados; vacío por defecto
     */
    default Set<String> subprotocols() {
        return Set.of();
    }
}
