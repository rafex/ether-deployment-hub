package dev.rafex.ether.http.security;

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.rafex.ether.http.security.ip.IpPolicy;
import dev.rafex.ether.http.security.profile.HttpSecurityProfile;

class HttpSecurityProfileTest {

    @Test
    void ipPolicyShouldRespectAllowAndDenyRules() {
        final var policy = new IpPolicy(List.of("10.0.", "192.168.1.10"), List.of("10.0.0.5"));

        assertTrue(policy.isAllowed("10.0.0.7"));
        assertFalse(policy.isAllowed("10.0.0.5"));
    }

    @Test
    void rateLimitShouldBeDisabledByDefault() {
        final var profile = HttpSecurityProfile.defaults();

        assertFalse(profile.rateLimit().isEnabled());
    }
}
