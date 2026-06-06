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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import dev.rafex.ether.http.client.EtherHttpClient;
import dev.rafex.ether.http.client.model.HttpRequestSpec;
import dev.rafex.ether.http.client.model.HttpResponseSpec;
import dev.rafex.ether.webhook.client.WebhookDeliveryClient;
import dev.rafex.ether.webhook.crypto.HmacWebhookSignerVerifier;
import dev.rafex.ether.webhook.headers.WebhookHeaders;
import dev.rafex.ether.webhook.model.WebhookPayload;

class WebhookDeliveryClientTest {

    @Test
    void shouldBuildSignedRequest() {
        final var signing = new HmacWebhookSignerVerifier("secret".getBytes());
        final var payload = WebhookPayload.ofJson("delivery-7", "order.created", Map.of("orderId", 7))
                .withHeader("X-Custom", "yes");
        final var client = new WebhookDeliveryClient(new RecordingHttpClient(), signing);

        final var request = client.buildRequest(URI.create("https://example.com/webhook"), payload);

        assertEquals("delivery-7", request.headers().get(WebhookHeaders.DELIVERY_ID).get(0));
        assertEquals("order.created", request.headers().get(WebhookHeaders.EVENT_TYPE).get(0));
        assertTrue(request.headers().containsKey(WebhookHeaders.SIGNATURE));
        assertEquals("yes", request.headers().get("X-Custom").get(0));
    }

    private static final class RecordingHttpClient implements EtherHttpClient {
        @Override
        public HttpResponseSpec send(final HttpRequestSpec request) throws IOException, InterruptedException {
            return new HttpResponseSpec(202, Map.of(), new byte[0]);
        }

        @Override
        public <T> T sendJson(final HttpRequestSpec request, final TypeReference<T> typeReference)
                throws IOException, InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T sendJson(final HttpRequestSpec request, final Class<T> type)
                throws IOException, InterruptedException {
            throw new UnsupportedOperationException();
        }
    }
}
