package dev.rafex.ether.http.security.cors;

/*-
 * #%L
 * ether-http-security
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Política de seguridad CORS para controlar el acceso entre orígenes.
 * <p>
 * Configura los encabezados HTTP CORS que permiten o restringen el acceso
 * a recursos desde dominios diferentes al servidor.
 * </p>
 *
 * @param allowAnyOrigin Permite cualquier origen (riesgo de seguridad, usar solo en desarrollo)
 * @param allowedOrigins Lista de orígenes permitidos explícitamente
 * @param allowedMethods Métodos HTTP permitidos (GET, POST, etc.)
 * @param allowedHeaders Headers permitidos en las solicitudes
 * @param exposedHeaders Headers expuestos en la respuesta
 * @param allowCredentials Indica si se permiten credenciales (cookies, tokens)
 * @param maxAgeSeconds Tiempo de cache de la preflight request
 * @param varyOrigin Agrega header Vary: Origin para evitar cache incorrecto
 */
public record CorsPolicy(boolean allowAnyOrigin, List<String> allowedOrigins, List<String> allowedMethods,
        List<String> allowedHeaders, List<String> exposedHeaders, boolean allowCredentials, int maxAgeSeconds,
        boolean varyOrigin) {

    public CorsPolicy {
        allowedOrigins = copyList(allowedOrigins);
        allowedMethods = copyList(allowedMethods);
        allowedHeaders = copyList(allowedHeaders);
        exposedHeaders = copyList(exposedHeaders);
    }

    public static CorsPolicy permissive() {
        return new CorsPolicy(true, List.of(), defaultMethods(), List.of("*"), List.of(), false, 3600, true);
    }

    public static CorsPolicy strict(final List<String> allowedOrigins) {
        return new CorsPolicy(false, allowedOrigins, defaultMethods(), List.of("content-type", "authorization"),
                List.of(), false, 3600, true);
    }

    public boolean isOriginAllowed(final String origin) {
        if (allowAnyOrigin) {
            return true;
        }
        if (origin == null || origin.isBlank()) {
            return false;
        }
        for (final var allowed : allowedOrigins) {
            if (matches(allowed, origin)) {
                return true;
            }
        }
        return false;
    }

    public Map<String, String> responseHeaders(final String origin) {
        final var headers = new LinkedHashMap<String, String>();
        if (allowAnyOrigin) {
            headers.put("Access-Control-Allow-Origin", "*");
        } else if (isOriginAllowed(origin)) {
            headers.put("Access-Control-Allow-Origin", origin);
        }
        headers.put("Access-Control-Allow-Methods", String.join(", ", allowedMethods));
        headers.put("Access-Control-Allow-Headers", String.join(", ", allowedHeaders));
        if (!exposedHeaders.isEmpty()) {
            headers.put("Access-Control-Expose-Headers", String.join(", ", exposedHeaders));
        }
        if (allowCredentials) {
            headers.put("Access-Control-Allow-Credentials", "true");
        }
        headers.put("Access-Control-Max-Age", Integer.toString(maxAgeSeconds));
        if (varyOrigin) {
            headers.put("Vary", "Origin");
        }
        return headers;
    }

    private static List<String> copyList(final List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static boolean matches(final String allowed, final String candidate) {
        if (allowed == null || candidate == null) {
            return false;
        }
        if ("*".equals(allowed)) {
            return true;
        }
        return Objects.equals(allowed, candidate);
    }

    private static List<String> defaultMethods() {
        return List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD");
    }
}
