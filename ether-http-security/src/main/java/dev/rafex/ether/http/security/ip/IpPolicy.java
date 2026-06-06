package dev.rafex.ether.http.security.ip;

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
 * Política de control de acceso basada en direcciones IP.
 * <p>
 * Permite definir listas de permitidos y denegados para controlar
 * qué direcciones IP pueden acceder a los recursos.
 * </p>
 */
public record IpPolicy(List<String> allowList, List<String> denyList) {

    public IpPolicy {
        allowList = allowList == null ? List.of() : List.copyOf(allowList);
        denyList = denyList == null ? List.of() : List.copyOf(denyList);
    }

    public static IpPolicy allowAll() {
        return new IpPolicy(List.of(), List.of());
    }

    public boolean isAllowed(final String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        for (final var deny : denyList) {
            if (matches(deny, ip)) {
                return false;
            }
        }
        if (allowList.isEmpty()) {
            return true;
        }
        for (final var allow : allowList) {
            if (matches(allow, ip)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(final String rule, final String ip) {
        if (rule == null || rule.isBlank()) {
            return false;
        }
        if ("*".equals(rule)) {
            return true;
        }
        return ip.equals(rule) || ip.startsWith(rule);
    }
}
