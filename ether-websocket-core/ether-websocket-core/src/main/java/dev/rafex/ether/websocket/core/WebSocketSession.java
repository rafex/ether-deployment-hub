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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * Representa una sesión WebSocket activa. Proporciona acceso a los metadatos de la conexión
 * (identificador, path, headers, parámetros) y métodos para enviar mensajes y cerrar la sesión.
 */
public interface WebSocketSession {

    /**
     * Devuelve el identificador único de esta sesión.
     *
     * @return cadena que identifica unívocamente la sesión
     */
    String id();

    /**
     * Devuelve el path de la petición WebSocket que originó esta sesión.
     *
     * @return path de la URI de la conexión
     */
    String path();

    /**
     * Devuelve el subprotocolo WebSocket negociado durante el handshake.
     *
     * @return nombre del subprotocolo seleccionado, o cadena vacía si no se negoció
     */
    String subprotocol();

    /**
     * Indica si la conexión WebSocket sigue abierta.
     *
     * @return {@code true} si la sesión está activa; {@code false} si ya se cerró
     */
    boolean isOpen();

    /**
     * Devuelve el valor del parámetro de path con el nombre especificado.
     *
     * @param name nombre del parámetro de path
     * @return valor del parámetro, o {@code null} si no existe
     */
    String pathParam(String name);

    /**
     * Devuelve el primer valor del parámetro de query con el nombre especificado.
     *
     * @param name nombre del parámetro de query
     * @return primer valor del parámetro, o {@code null} si no existe
     */
    String queryFirst(String name);

    /**
     * Devuelve todos los valores del parámetro de query con el nombre especificado.
     *
     * @param name nombre del parámetro de query
     * @return lista de valores asociados; vacía si el parámetro no existe
     */
    List<String> queryAll(String name);

    /**
     * Devuelve el primer valor del header HTTP con el nombre especificado.
     *
     * @param name nombre del header (case-insensitive)
     * @return primer valor del header, o {@code null} si no existe
     */
    String headerFirst(String name);

    /**
     * Devuelve un atributo almacenado en esta sesión bajo el nombre dado.
     *
     * @param name clave del atributo
     * @return valor del atributo, o {@code null} si no existe
     */
    Object attribute(String name);

    /**
     * Almacena un atributo en esta sesión bajo el nombre dado.
     *
     * @param name  clave del atributo
     * @param value valor a almacenar
     */
    void attribute(String name, Object value);

    /**
     * Devuelve todos los parámetros de path extraídos durante el matching de ruta.
     *
     * @return mapa inmutable de nombres de parámetro a sus valores
     */
    Map<String, String> pathParams();

    /**
     * Devuelve todos los parámetros de query de la URI de la conexión.
     *
     * @return mapa de nombres de parámetro a listas de valores
     */
    Map<String, List<String>> queryParams();

    /**
     * Devuelve todos los headers HTTP de la petición de handshake.
     *
     * @return mapa de nombres de header a listas de valores
     */
    Map<String, List<String>> headers();

    /**
     * Envía un mensaje de texto al cliente conectado.
     *
     * @param text contenido de texto a enviar
     * @return {@link CompletionStage} que se completa cuando el mensaje se envía
     */
    CompletionStage<Void> sendText(String text);

    /**
     * Envía un mensaje binario al cliente conectado.
     *
     * @param data contenido binario a enviar
     * @return {@link CompletionStage} que se completa cuando el mensaje se envía
     */
    CompletionStage<Void> sendBinary(ByteBuffer data);

    /**
     * Cierra la conexión WebSocket con el estado de cierre especificado.
     *
     * @param status estado de cierre a enviar al cliente
     * @return {@link CompletionStage} que se completa cuando la conexión se cierra
     */
    CompletionStage<Void> close(WebSocketCloseStatus status);
}
