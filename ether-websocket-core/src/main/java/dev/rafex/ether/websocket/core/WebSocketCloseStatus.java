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

/**
 * Representa un estado de cierre de una conexión WebSocket conforme a la especificación RFC 6455.
 * Cada estado se compone de un código numérico y una razón descriptiva.
 */
public record WebSocketCloseStatus(int code, String reason) {

    /** Cierre normal: la conexión se completó con éxito. */
    public static final WebSocketCloseStatus NORMAL = new WebSocketCloseStatus(1000, "normal");
    /** El punto de partida se aleja (el servidor se apaga o el cliente navega fuera). */
    public static final WebSocketCloseStatus GOING_AWAY = new WebSocketCloseStatus(1001, "going_away");
    /** La conexión se cierra debido a una violación del protocolo. */
    public static final WebSocketCloseStatus PROTOCOL_ERROR = new WebSocketCloseStatus(1002, "protocol_error");
    /** Se recibió un tipo de mensaje que no es aceptable. */
    public static final WebSocketCloseStatus NOT_ACCEPTABLE = new WebSocketCloseStatus(1003, "not_acceptable");
    /** El servidor encontró un error inesperado que impide continuar. */
    public static final WebSocketCloseStatus SERVER_ERROR = new WebSocketCloseStatus(1011, "server_error");

    public WebSocketCloseStatus {
        reason = reason == null ? "" : reason;
    }

    /**
     * Crea un estado de cierre con el código y la razón especificados.
     *
     * @param code   código de cierre numérico según RFC 6455
     * @param reason descripción legible del motivo de cierre; se normaliza a cadena vacía si es {@code null}
     * @return nueva instancia de {@code WebSocketCloseStatus}
     */
    public static WebSocketCloseStatus of(final int code, final String reason) {
        return new WebSocketCloseStatus(code, reason);
    }
}
