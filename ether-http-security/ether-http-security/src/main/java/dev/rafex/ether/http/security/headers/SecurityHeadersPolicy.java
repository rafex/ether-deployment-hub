package dev.rafex.ether.http.security.headers;

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
import java.util.Map;

/**
 * Política de seguridad para encabezados HTTP.
 * <p>
 * Configura encabezados de seguridad como CSP, HSTS, X-Frame-Options,
 * para proteger contra ataques comunes como XSS, clickjacking, etc.
 * </p>
 */
public record SecurityHeadersPolicy(boolean contentTypeOptions, boolean frameOptions, boolean referrerPolicy,
        boolean permissionsPolicy, boolean hsts, boolean noStore, String contentSecurityPolicy,
        Map<String, String> customHeaders) {

    public SecurityHeadersPolicy {
        customHeaders = customHeaders == null ? Map.of() : Map.copyOf(customHeaders);
    }

    public static SecurityHeadersPolicy defaults() {
        return new SecurityHeadersPolicy(true, true, true, true, true, true,
                "default-src 'self'; frame-ancestors 'none'; base-uri 'self'", Map.of());
    }

    public Map<String, String> headers() {
        final var headers = new LinkedHashMap<String, String>();
        if (contentTypeOptions) {
            headers.put("X-Content-Type-Options", "nosniff");
        }
        if (frameOptions) {
            headers.put("X-Frame-Options", "DENY");
        }
        if (referrerPolicy) {
            headers.put("Referrer-Policy", "no-referrer");
        }
        if (permissionsPolicy) {
            headers.put("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        }
        if (hsts) {
            headers.put("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
        if (noStore) {
            headers.put("Cache-Control", "no-store");
        }
        if (contentSecurityPolicy != null && !contentSecurityPolicy.isBlank()) {
            headers.put("Content-Security-Policy", contentSecurityPolicy);
        }
        headers.putAll(customHeaders);
        return headers;
    }
}
