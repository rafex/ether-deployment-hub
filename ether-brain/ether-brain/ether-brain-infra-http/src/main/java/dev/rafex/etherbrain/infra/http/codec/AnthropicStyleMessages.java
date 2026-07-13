package dev.rafex.etherbrain.infra.http.codec;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.rafex.etherbrain.ports.model.Message;

/**
 * Helpers for building message content shared by Anthropic-style codecs.
 *
 * <p>Both {@link AnthropicCodec} and {@link BedrockCodec} (which speaks the
 * Bedrock "messages" API modeled on Anthropic) represent a tool result as a
 * {@code user} message carrying a {@code tool_result} content block.
 */
final class AnthropicStyleMessages {

    private AnthropicStyleMessages() {
    }

    /**
     * Appends a {@code user} message with a single {@code tool_result} block to
     * {@code messages}.
     *
     * @param messages the JSON array of messages to append to
     * @param msg      the domain tool-result message (its {@code toolCallId} and
     *                 {@code content} are used)
     */
    static void addToolResult(ArrayNode messages, Message msg) {
        ObjectNode m = messages.addObject();
        m.put("role", "user");
        ArrayNode content = m.putArray("content");
        ObjectNode result = content.addObject();
        result.put("type", "tool_result");
        result.put("tool_use_id", msg.toolCallId() != null ? msg.toolCallId() : "");
        result.put("content", msg.content());
    }
}
