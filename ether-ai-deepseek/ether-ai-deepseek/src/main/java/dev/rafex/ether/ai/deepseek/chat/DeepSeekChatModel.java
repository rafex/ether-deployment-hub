package dev.rafex.ether.ai.deepseek.chat;

/*-
 * #%L
 * ether-ai-deepseek
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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;

import dev.rafex.ether.ai.core.chat.AiChatModel;
import dev.rafex.ether.ai.core.chat.AiChatRequest;
import dev.rafex.ether.ai.core.chat.AiChatResponse;
import dev.rafex.ether.ai.core.error.AiHttpException;
import dev.rafex.ether.ai.core.message.AiMessage;
import dev.rafex.ether.ai.core.message.AiMessageRole;
import dev.rafex.ether.ai.core.usage.AiUsage;
import dev.rafex.ether.ai.deepseek.config.DeepSeekConfig;
import dev.rafex.ether.json.JsonCodec;
import dev.rafex.ether.json.JsonUtils;

/**
 * Chat model adapter for the DeepSeek API.
 *
 * <p>Implements {@link AiChatModel} by sending HTTP requests to the
 * DeepSeek chat completions endpoint and mapping JSON responses back to
 * Ether AI domain objects.
 */
public final class DeepSeekChatModel implements AiChatModel {

    /** Configuration holding API key, base URI, timeout and default headers. */
    private final DeepSeekConfig config;

    /** HTTP client used to send requests to the DeepSeek API. */
    private final HttpClient httpClient;

    /** JSON codec used to serialize payloads and deserialize responses. */
    private final JsonCodec jsonCodec;

    /**
     * Creates a new model with a default HTTP client and JSON codec.
     *
     * @param config DeepSeek connection configuration
     */
    public DeepSeekChatModel(final DeepSeekConfig config) {
        this(config, HttpClient.newBuilder().connectTimeout(config.timeout()).build(), JsonUtils.codec());
    }

    /**
     * Creates a new model with a custom HTTP client and JSON codec.
     *
     * @param config     DeepSeek connection configuration
     * @param httpClient pre-configured HTTP client to use for requests
     * @param jsonCodec  JSON codec for serialization; falls back to the default codec when {@code null}
     */
    public DeepSeekChatModel(final DeepSeekConfig config, final HttpClient httpClient, final JsonCodec jsonCodec) {
        this.config = Objects.requireNonNull(config, "config");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.jsonCodec = jsonCodec == null ? JsonUtils.codec() : jsonCodec;
    }

    /**
     * Sends a chat completion request to the DeepSeek API and returns the parsed response.
     *
     * @param request the chat request containing model, messages and generation parameters
     * @return the parsed response including the assistant message, model name and token usage
     * @throws IOException          if an I/O error occurs during the HTTP call or response parsing
     * @throws InterruptedException if the current thread is interrupted while waiting for the HTTP response
     * @throws AiHttpException      if the DeepSeek API returns a non-2xx HTTP status code
     */
    @Override
    public AiChatResponse generate(final AiChatRequest request) throws IOException, InterruptedException {
        final byte[] payload = jsonCodec.toJsonBytes(toPayload(request));
        final var builder = HttpRequest.newBuilder(config.chatCompletionsUri()).timeout(config.timeout())
                .header("Authorization", "Bearer " + config.apiKey()).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload));
        config.defaultHeaders().forEach(builder::header);

        final var response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AiHttpException("DeepSeek request failed with HTTP " + response.statusCode(),
                    response.statusCode(), new String(response.body(), StandardCharsets.UTF_8));
        }

        final JsonNode root = jsonCodec.readTree(response.body());
        final JsonNode choice = root.path("choices").path(0);
        final JsonNode messageNode = choice.path("message");
        final var message = new AiMessage(AiMessageRole.fromWireValue(text(messageNode, "role")),
                text(messageNode, "content"));
        return new AiChatResponse(text(root, "id"), text(root, "model"), message, text(choice, "finish_reason"),
                usage(root.path("usage")));
    }

    /**
     * Converts an {@link AiChatRequest} into a JSON-serializable payload map
     * compatible with the DeepSeek chat completions API.
     */
    private static Map<String, Object> toPayload(final AiChatRequest request) {
        final var payload = new LinkedHashMap<String, Object>();
        payload.put("model", request.model());
        payload.put("messages", request.messages().stream()
                .map(message -> Map.of("role", message.role().wireValue(), "content", message.content())).toList());
        if (request.temperature() != null) {
            payload.put("temperature", request.temperature());
        }
        if (request.maxOutputTokens() != null) {
            payload.put("max_tokens", request.maxOutputTokens());
        }
        return payload;
    }

    /**
     * Extracts a text value from a JSON node by field name.
     *
     * @throws IOException if the field is missing or {@code null} in the JSON tree
     */
    private static String text(final JsonNode node, final String fieldName) throws IOException {
        final JsonNode field = node.path(fieldName);
        if (field.isMissingNode() || field.isNull()) {
            throw new IOException("Missing JSON field: " + fieldName);
        }
        return field.asText();
    }

    /**
     * Parses token usage information from a JSON node, returning an empty
     * usage object when the node is absent or {@code null}.
     */
    private static AiUsage usage(final JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return AiUsage.empty();
        }
        return new AiUsage(node.path("prompt_tokens").asInt(0), node.path("completion_tokens").asInt(0),
                node.path("total_tokens").asInt(0));
    }
}
