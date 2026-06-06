package dev.rafex.ether.config.internal;

/*-
 * #%L
 * ether-config
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StructuredConfigSupport {

    private StructuredConfigSupport() {
    }

    public static Map<String, String> flatten(final Map<String, Object> input) {
        final Map<String, String> output = new LinkedHashMap<>();
        flattenInto(output, "", input);
        return Map.copyOf(output);
    }

    @SuppressWarnings("unchecked")
    private static void flattenInto(final Map<String, String> output, final String prefix, final Object value) {
        switch (value) {
        case null -> {
            return;
        }
        case final Map<?, ?> map -> {
            for (final var entry : map.entrySet()) {
                final var key = prefix.isBlank() ? String.valueOf(entry.getKey()) : prefix + "." + entry.getKey();
                flattenInto(output, key, entry.getValue());
            }
            return;
        }
        case final List<?> list -> {
            for (var i = 0; i < list.size(); i++) {
                flattenInto(output, prefix + "[" + i + "]", list.get(i));
            }
            return;
        }
        case final Enum<?> enumValue -> {
            output.put(prefix, enumValue.name());
            return;
        }
        default -> {
        }
        }
        output.put(prefix, String.valueOf(value));
    }
}
