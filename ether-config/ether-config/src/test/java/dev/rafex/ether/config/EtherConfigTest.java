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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.rafex.ether.config.sources.MapConfigSource;

class EtherConfigTest {

    @Test
    void shouldResolveValuesBySourcePrecedence() {
        final var config = EtherConfig.of(new MapConfigSource("env", Map.of("PORT", "8080")),
                new MapConfigSource("defaults", Map.of("PORT", "9090", "HOST", "localhost")));

        assertEquals("8080", config.require("PORT"));
        assertEquals("localhost", config.require("HOST"));
    }

    @Test
    void snapshotShouldExposeMergedValues() {
        final var config = EtherConfig.of(new MapConfigSource("first", Map.of("A", "1")),
                new MapConfigSource("second", Map.of("A", "2", "B", "3")));

        assertEquals(Map.of("A", "1", "B", "3"), config.snapshot());
    }
}
