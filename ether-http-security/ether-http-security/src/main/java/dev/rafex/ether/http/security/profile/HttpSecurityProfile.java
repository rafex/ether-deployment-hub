package dev.rafex.ether.http.security.profile;

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

import dev.rafex.ether.http.security.cors.CorsPolicy;
import dev.rafex.ether.http.security.headers.SecurityHeadersPolicy;
import dev.rafex.ether.http.security.ip.IpPolicy;
import dev.rafex.ether.http.security.proxy.TrustedProxyPolicy;
import dev.rafex.ether.http.security.ratelimit.RateLimitPolicy;

/**
 * Perfil de seguridad HTTP que agrupa todas las políticas de seguridad.
 * <p>
 * Define la configuración completa de seguridad para un servidor HTTP,
 * incluyendo CORS, encabezados de seguridad, control de IPs y rate limiting.
 * </p>
 */
public record HttpSecurityProfile(CorsPolicy cors, SecurityHeadersPolicy headers, TrustedProxyPolicy trustedProxies,
        IpPolicy ipPolicy, RateLimitPolicy rateLimit) {

    public static HttpSecurityProfile defaults() {
        return new HttpSecurityProfile(CorsPolicy.strict(java.util.List.of()), SecurityHeadersPolicy.defaults(),
                TrustedProxyPolicy.disabled(), IpPolicy.allowAll(),
                new RateLimitPolicy(RateLimitPolicy.Scope.GLOBAL, 0, 0, 0, 0));
    }
}
