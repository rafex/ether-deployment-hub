package dev.rafex.ether.webhook.model;

/*-
 * #%L
 * ether-webhook
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

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.rafex.ether.http.client.model.HttpMethod;
import dev.rafex.ether.http.client.model.HttpRequestSpec;
import dev.rafex.ether.json.JsonUtils;

/**
 * Payload de un webhook.
 * Es un record inmutable que contiene todos los datos necesarios
 * para enviar un webhook a un endpoint externo.
 * 
 * @param deliveryId ID de entrega único
 * @param eventType tipo de evento
 * @param occurredAt momento en que ocurrió el evento
 * @param contentType tipo de contenido del cuerpo
 * @param body cuerpo del payload en bytes
 * @param headers cabeceras HTTP adicionales
 */
public record WebhookPayload(String deliveryId, String eventType, Instant occurredAt, String contentType, byte[] body,
        Map<String, List<String>> headers) {

    public WebhookPayload {
        body = body == null ? new byte[0] : body.clone();
        headers = normalizeHeaders(headers);
    }

    /**
     * Crea un payload con contenido JSON.
     * 
     * @param deliveryId ID de entrega
     * @param eventType tipo de evento
     * @param value objeto a serializar como JSON
     * @return el payload creado
     */
    public static WebhookPayload ofJson(final String deliveryId, final String eventType, final Object value) {
        return new WebhookPayload(deliveryId, eventType, Instant.now(), "application/json",
                JsonUtils.toJsonBytes(value), Map.of());
    }

    /**
     * Crea un payload con contenido de texto plano.
     * 
     * @param deliveryId ID de entrega
     * @param eventType tipo de evento
     * @param value contenido de texto
     * @return el payload creado
     */
    public static WebhookPayload ofText(final String deliveryId, final String eventType, final String value) {
        return new WebhookPayload(deliveryId, eventType, Instant.now(), "text/plain; charset=utf-8",
                value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8), Map.of());
    }

    /**
     * Añade una cabecera al payload.
     * 
     * @param name nombre de la cabecera
     * @param value valor de la cabecera
     * @return un nuevo payload con la cabecera añadida
     */
    public WebhookPayload withHeader(final String name, final String value) {
        final var copy = new LinkedHashMap<String, List<String>>();
        copy.putAll(headers);
        copy.put(name, List.of(value));
        return new WebhookPayload(deliveryId, eventType, occurredAt, contentType, body, copy);
    }

    /**
     * Convierte el payload a una especificación de petición HTTP.
     * 
     * @param endpoint URL del endpoint
     * @return la especificación de petición
     */
    public HttpRequestSpec toRequest(final URI endpoint) {
        final var builder = HttpRequestSpec.builder(HttpMethod.POST, endpoint).body(body).contentType(contentType);
        for (final var entry : headers.entrySet()) {
            for (final var value : entry.getValue()) {
                builder.header(entry.getKey(), value);
            }
        }
        return builder.build();
    }

    private static Map<String, List<String>> normalizeHeaders(final Map<String, List<String>> raw) {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        final var copy = new LinkedHashMap<String, List<String>>();
        for (final var entry : raw.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Map.copyOf(copy);
    }
}
