package dev.rafex.ether.http.security.ratelimit;

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

/**
 * Política de control de tasa (rate limiting) para prevenir abusos.
 * <p>
 * Limita la cantidad de solicitudes por ventana de tiempo para proteger
 * contra ataques de fuerza bruta o abuso de recursos.
 * </p>
 */
public record RateLimitPolicy(Scope scope, int maxRequests, int windowSeconds, int burst, int maxConcurrentRequests) {

    /**
     * Alcance de la política de rate limiting.
     * <ul>
     *   <li>GLOBAL: Un límite compartido por todos los clientes</li>
     *   <li>PER_IP: Un límite por cada dirección IP</li>
     * </ul>
     */
    public enum Scope {
        GLOBAL, PER_IP
    }

    /**
     * Verifica si el rate limiting está habilitado.
     * @return true si maxRequests > 0 y windowSeconds > 0
     */
    public boolean isEnabled() {
        return maxRequests > 0 && windowSeconds > 0;
    }
}
