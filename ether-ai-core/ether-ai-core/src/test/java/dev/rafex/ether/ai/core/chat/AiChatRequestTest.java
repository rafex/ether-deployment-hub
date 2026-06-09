package dev.rafex.ether.ai.core.chat;

/*-
 * #%L
 * ether-ai-core
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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.rafex.ether.ai.core.message.AiMessage;

class AiChatRequestTest {

    @Test
    void shouldCopyMessagesDefensively() {
        final var messages = new ArrayList<>(List.of(AiMessage.user("hola")));
        final var request = new AiChatRequest("gpt-test", messages, 0.3d, 128);

        messages.add(AiMessage.user("otro"));

        assertEquals(1, request.messages().size());
        assertEquals("hola", request.messages().get(0).content());
    }

    @Test
    void shouldRejectInvalidArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> new AiChatRequest("", List.of(AiMessage.user("hola")), null, null));
        assertThrows(IllegalArgumentException.class, () -> new AiChatRequest("model", List.of(), null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new AiChatRequest("model", List.of(AiMessage.user("hola")), 3.0d, null));
        assertThrows(IllegalArgumentException.class,
                () -> new AiChatRequest("model", List.of(AiMessage.user("hola")), null, 0));
    }
}
