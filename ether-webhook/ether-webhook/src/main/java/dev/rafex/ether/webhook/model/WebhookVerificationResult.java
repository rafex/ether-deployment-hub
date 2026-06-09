package dev.rafex.ether.webhook.model;

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

/**
 * Resultado de la verificación de una firma de webhook.
 * Es un record inmutable que indica si la verificación fue exitosa
 * y proporciona detalles en caso de fallo.
 * 
 * @param valid true si la firma es válida
 * @param reason motivo del fallo (si aplica)
 * @param signature la firma verificada
 */
public record WebhookVerificationResult(boolean valid, String reason, WebhookSignature signature) {

    /**
     * Crea un resultado de verificación exitosa.
     * 
     * @param signature la firma verificada
     * @return el resultado de verificación
     */
    public static WebhookVerificationResult ok(final WebhookSignature signature) {
        return new WebhookVerificationResult(true, null, signature);
    }

    /**
     * Crea un resultado de verificación fallida.
     * 
     * @param reason motivo del fallo
     * @param signature la firma (puede ser null)
     * @return el resultado de verificación
     */
    public static WebhookVerificationResult failed(final String reason, final WebhookSignature signature) {
        return new WebhookVerificationResult(false, reason, signature);
    }
}
