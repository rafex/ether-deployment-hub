package dev.rafex.ether.http.security.proxy;

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

import java.util.List;

/**
 * Política para configurar proxy de confianza en servidores.
 * <p>
 * Define qué proxies son de confianza y cómo extraer la IP real del cliente
 * cuando hay proxies o balanceadores de carga en medio.
 * </p>
 */
public record TrustedProxyPolicy(List<String> trustedSources, boolean trustForwardedHeader, boolean forwardedOnly,
        boolean preferRightMostForwardedFor) {

    public TrustedProxyPolicy {
        trustedSources = trustedSources == null ? List.of() : List.copyOf(trustedSources);
    }

    public static TrustedProxyPolicy disabled() {
        return new TrustedProxyPolicy(List.of(), false, false, true);
    }

    public boolean isTrusted(final String remoteAddress) {
        if (remoteAddress == null || remoteAddress.isBlank()) {
            return false;
        }
        for (final var source : trustedSources) {
            if (remoteAddress.equals(source) || remoteAddress.startsWith(source)) {
                return true;
            }
        }
        return false;
    }
}
