package dev.rafex.ether.webhook.client;

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

import java.io.IOException;
import java.net.URI;

import dev.rafex.ether.http.client.EtherHttpClient;
import dev.rafex.ether.http.client.model.HttpMethod;
import dev.rafex.ether.http.client.model.HttpRequestSpec;
import dev.rafex.ether.http.client.model.HttpResponseSpec;
import dev.rafex.ether.webhook.api.WebhookSigner;
import dev.rafex.ether.webhook.headers.WebhookHeaders;
import dev.rafex.ether.webhook.model.WebhookPayload;

public final class WebhookDeliveryClient {

    private final EtherHttpClient httpClient;
    private final WebhookSigner signer;

    public WebhookDeliveryClient(final EtherHttpClient httpClient, final WebhookSigner signer) {
        this.httpClient = httpClient;
        this.signer = signer;
    }

    public HttpRequestSpec buildRequest(final URI endpoint, final WebhookPayload payload) {
        final var signature = signer.sign(payload);
        final var builder = HttpRequestSpec.builder(HttpMethod.POST, endpoint).body(payload.body())
                .contentType(payload.contentType()).header(WebhookHeaders.DELIVERY_ID, payload.deliveryId())
                .header(WebhookHeaders.EVENT_TYPE, payload.eventType())
                .header(WebhookHeaders.TIMESTAMP, Long.toString(signature.timestampEpochMilli()))
                .header(WebhookHeaders.ALGORITHM, signature.algorithm())
                .header(WebhookHeaders.SIGNATURE, signature.value());
        for (final var entry : payload.headers().entrySet()) {
            for (final var value : entry.getValue()) {
                builder.header(entry.getKey(), value);
            }
        }
        return builder.build();
    }

    public HttpResponseSpec send(final URI endpoint, final WebhookPayload payload)
            throws IOException, InterruptedException {
        return httpClient.send(buildRequest(endpoint, payload));
    }
}
