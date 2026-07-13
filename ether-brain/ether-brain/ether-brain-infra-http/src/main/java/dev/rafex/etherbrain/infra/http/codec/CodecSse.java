package dev.rafex.etherbrain.infra.http.codec;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Stream;

/**
 * SSE (Server-Sent Events) helpers shared by the provider codecs.
 *
 * <p>The three codecs that support streaming ({@link OpenAiCodec},
 * {@link AnthropicCodec}, {@link GeminiCodec}) all share the same wire-level
 * boilerplate: send a POST, check for 200, then iterate over {@code data:}
 * lines. This class centralises those steps so each codec focuses on
 * provider-specific chunk parsing.
 */
final class CodecSse {

    private CodecSse() {
    }

    /**
     * Sends {@code request} and returns the line stream if the status is 200.
     *
     * @param httpClient the shared HTTP client
     * @param request    the pre-built POST request
     * @param provider   human-readable provider name used in the error message
     * @return the response body as a lazily-read stream of lines
     * @throws IOException          on I/O failure
     * @throws InterruptedException on thread interruption
     * @throws RuntimeException     if the HTTP status is not 200 (message includes
     *                              the provider name and response body)
     */
    static Stream<String> sendAndCheckLines(final HttpClient httpClient, final HttpRequest request,
            final String provider) throws IOException, InterruptedException {
        final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofLines());
        if (response.statusCode() != 200) {
            final var errorBody = response.body().filter(l -> !l.isBlank()).reduce("", (a, b) -> a + b);
            throw new RuntimeException(
                    "%s returned HTTP %d: %s".formatted(provider, response.statusCode(), errorBody));
        }
        return response.body();
    }

    /**
     * Extracts the {@code data:} payload from an SSE line.
     *
     * @param line the raw line from the stream (may be blank or non-data)
     * @return the trimmed payload after {@code data:}, or {@code null} if the
     *         line does not carry a data event
     */
    static String dataPayload(final String line) {
        if (line == null || !line.startsWith("data: ")) {
            return null;
        }
        return line.substring(6).trim();
    }
}
