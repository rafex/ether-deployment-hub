package dev.rafex.etherbrain.tools.remote;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Shared HTTP helpers for the faiss-poc remote tools.
 *
 * <p>Centralises two concerns that were previously duplicated across
 * {@link FaissTokenManager}, {@link FaissMemoryProvider},
 * {@link KnowledgeSearchTool} and {@link MemoryCommitTool}:
 * <ul>
 *   <li>Building an {@link HttpClient} with an optional trust-all TLS context
 *       (for self-signed certificates).</li>
 *   <li>Applying the correct authentication header for a token: JWTs
 *       ({@code eyJ…}) use {@code Authorization: Bearer}; anything else uses
 *       {@code X-API-Key}.</li>
 * </ul>
 */
final class RemoteHttp {

    private RemoteHttp() {
    }

    /**
     * Builds an {@link HttpClient} with a 10s connect timeout.
     *
     * @param skipTlsVerify {@code true} to accept self-signed certificates via a
     *                      trust-all {@link SSLContext}
     * @return the configured client
     */
    static HttpClient buildHttpClient(boolean skipTlsVerify) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10));

        if (skipTlsVerify) {
            try {
                TrustManager[] trustAll = {new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(X509Certificate[] c, String a) {}
                    public void checkServerTrusted(X509Certificate[] c, String a) {}
                }};
                SSLContext ctx = SSLContext.getInstance("TLS");
                ctx.init(null, trustAll, new SecureRandom());
                builder.sslContext(ctx);
            } catch (Exception e) {
                throw new RuntimeException("Failed to build trust-all SSL context", e);
            }
        }

        return builder.build();
    }

    /**
     * Applies the appropriate auth header for {@code token} to {@code request}.
     *
     * <p>JWT tokens ({@code eyJ…}) are sent as {@code Authorization: Bearer};
     * any other non-blank token is sent as {@code X-API-Key}. A {@code null}
     * token adds no header.
     *
     * @param request the request builder to mutate
     * @param token   the auth token (may be {@code null})
     */
    static void applyAuth(HttpRequest.Builder request, String token) {
        if (token != null && token.startsWith("eyJ")) {
            request.header("Authorization", "Bearer " + token);
        } else if (token != null && !token.isBlank()) {
            request.header("X-API-Key", token);
        }
    }
}
