package dev.rafex.ether.config.sources;

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
import java.util.Locale;
import java.util.Map;

public final class EnvironmentConfigSource extends MapConfigSource {

    public EnvironmentConfigSource() {
        this(System.getenv());
    }

    public EnvironmentConfigSource(final Map<String, String> env) {
        super("environment", normalize(env));
    }

    private static Map<String, String> normalize(final Map<String, String> env) {
        final var normalized = new LinkedHashMap<String, String>();
        for (final var entry : env.entrySet()) {
            normalized.put(entry.getKey(), entry.getValue());
            normalized.putIfAbsent(toDotKey(entry.getKey()), entry.getValue());
        }
        return normalized;
    }

    private static String toDotKey(final String key) {
        return key.toLowerCase(Locale.ROOT).replace('_', '.');
    }
}
