package dev.rafex.ether.config;

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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import dev.rafex.ether.config.binding.ConfigBinder;
import dev.rafex.ether.config.sources.ConfigSource;

public final class EtherConfig {

    private final List<ConfigSource> sources;

    private EtherConfig(final List<ConfigSource> sources) {
        this.sources = List.copyOf(sources);
    }

    public static EtherConfig of(final ConfigSource... sources) {
        return of(List.of(sources));
    }

    public static EtherConfig of(final List<ConfigSource> sources) {
        Objects.requireNonNull(sources, "sources");
        return new EtherConfig(new ArrayList<>(sources));
    }

    public List<ConfigSource> sources() {
        return sources;
    }

    public Optional<String> get(final String key) {
        for (final var source : sources) {
            final var value = source.get(key);
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    public String require(final String key) {
        return get(key).orElseThrow(() -> new IllegalArgumentException("Missing config key: " + key));
    }

    public Map<String, String> snapshot() {
        final var merged = new LinkedHashMap<String, String>();
        for (final var source : sources) {
            for (final var entry : source.entries().entrySet()) {
                merged.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
        return Map.copyOf(merged);
    }

    public <T extends Record> T bind(final Class<T> recordType) {
        return ConfigBinder.bind(this, recordType);
    }

    public <T extends Record> T bind(final String prefix, final Class<T> recordType) {
        return ConfigBinder.bind(this, prefix, recordType);
    }

    public <T extends Record> T bindValidated(final Class<T> recordType) {
        return ConfigBinder.bindValidated(this, recordType);
    }

    public <T extends Record> T bindValidated(final String prefix, final Class<T> recordType) {
        return ConfigBinder.bindValidated(this, prefix, recordType);
    }
}
