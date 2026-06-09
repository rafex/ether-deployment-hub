package dev.rafex.ether.webhook.crypto;

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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import dev.rafex.ether.webhook.api.WebhookSigner;
import dev.rafex.ether.webhook.api.WebhookVerifier;
import dev.rafex.ether.webhook.model.WebhookPayload;
import dev.rafex.ether.webhook.model.WebhookSignature;
import dev.rafex.ether.webhook.model.WebhookVerificationResult;

/**
 * Implementación de HMAC para firmar y verificar webhooks.
 * Utiliza HMAC-SHA256 por defecto para garantizar integridad y autenticidad.
 */
public final class HmacWebhookSignerVerifier implements WebhookSigner, WebhookVerifier {

    private static final String DEFAULT_ALGORITHM = "HmacSHA256";

    private final byte[] secret;
    private final String algorithm;
    private final Clock clock;

    /**
     * Crea un signer/verifier con secreto y algoritmo por defecto.
     * 
     * @param secret el secreto para HMAC
     */
    public HmacWebhookSignerVerifier(final byte[] secret) {
        this(secret, DEFAULT_ALGORITHM, Clock.systemUTC());
    }

    /**
     * Crea un signer/verifier con configuración completa.
     * 
     * @param secret el secreto para HMAC
     * @param algorithm el algoritmo HMAC (ej: HmacSHA256)
     * @param clock el reloj para marcas de tiempo
     */
    public HmacWebhookSignerVerifier(final byte[] secret, final String algorithm, final Clock clock) {
        this.secret = secret == null ? new byte[0] : secret.clone();
        this.algorithm = Objects.requireNonNullElse(algorithm, DEFAULT_ALGORITHM);
        this.clock = clock == null ? Clock.systemUTC() : clock;
        if (this.secret.length == 0) {
            throw new IllegalArgumentException("secret is required");
        }
    }

    /**
     * Firma un payload de webhook con HMAC.
     * 
     * @param payload el payload a firmar
     * @return la firma generada
     */
    @Override
    public WebhookSignature sign(final WebhookPayload payload) {
        final long timestamp = Instant.now(clock).toEpochMilli();
        return new WebhookSignature(algorithm, computeSignature(payload, timestamp), timestamp);
    }

    /**
     * Verifica la firma de un payload de webhook.
     * 
     * @param payload el payload a verificar
     * @param signature la firma a validar
     * @return el resultado de la verificación
     */
    @Override
    public WebhookVerificationResult verify(final WebhookPayload payload, final WebhookSignature signature) {
        if (signature == null) {
            return WebhookVerificationResult.failed("missing_signature", null);
        }
        if (!algorithm.equals(signature.algorithm())) {
            return WebhookVerificationResult.failed("unsupported_algorithm", signature);
        }

        final String expected = computeSignature(payload, signature.timestampEpochMilli());
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                signature.value().getBytes(StandardCharsets.UTF_8))) {
            return WebhookVerificationResult.failed("bad_signature", signature);
        }
        return WebhookVerificationResult.ok(signature);
    }

    /**
     * Calcula la firma HMAC para un payload.
     * 
     * @param payload el payload a firmar
     * @param timestamp la marca de tiempo
     * @return la firma en base64
     */
    private String computeSignature(final WebhookPayload payload, final long timestamp) {
        try {
            final var mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret, algorithm));
            mac.update(canonicalPayload(payload, timestamp).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal());
        } catch (final Exception e) {
            throw new IllegalStateException("unable to sign webhook payload", e);
        }
    }

    /**
     * Crea el payload canónico para la firma.
     * 
     * @param payload el payload original
     * @param timestamp la marca de tiempo
     * @return el payload canónico como string
     */
    private static String canonicalPayload(final WebhookPayload payload, final long timestamp) {
        return String.join("\n", nullToEmpty(payload.deliveryId()), nullToEmpty(payload.eventType()),
                nullToEmpty(payload.contentType()), Long.toString(timestamp),
                Base64.getUrlEncoder().withoutPadding().encodeToString(payload.body()));
    }

    /**
     * Convierte un valor nulo a string vacío.
     * 
     * @param value el valor a convertir
     * @return el valor o string vacío si es nulo
     */
    private static String nullToEmpty(final String value) {
        return value == null ? "" : value;
    }
}
