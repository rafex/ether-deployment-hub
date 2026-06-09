package dev.rafex.ether.webhook;

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.rafex.ether.webhook.crypto.HmacWebhookSignerVerifier;
import dev.rafex.ether.webhook.model.WebhookPayload;

class HmacWebhookSignerVerifierTest {

    @Test
    void shouldSignAndVerifyPayload() {
        final var clock = Clock.fixed(Instant.parse("2026-03-18T18:00:00Z"), ZoneOffset.UTC);
        final var codec = new HmacWebhookSignerVerifier("super-secret-key".getBytes(), "HmacSHA256", clock);
        final var payload = WebhookPayload.ofJson("delivery-1", "user.created", Map.of("id", 42));

        final var signature = codec.sign(payload);
        assertTrue(codec.verify(payload, signature).valid());
    }

    @Test
    void shouldRejectTamperedPayload() {
        final var clock = Clock.fixed(Instant.parse("2026-03-18T18:00:00Z"), ZoneOffset.UTC);
        final var codec = new HmacWebhookSignerVerifier("super-secret-key".getBytes(), "HmacSHA256", clock);
        final var payload = WebhookPayload.ofJson("delivery-1", "user.created", Map.of("id", 42));
        final var tampered = WebhookPayload.ofJson("delivery-1", "user.created", Map.of("id", 99));

        final var signature = codec.sign(payload);
        assertFalse(codec.verify(tampered, signature).valid());
    }
}
