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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import dev.rafex.ether.config.sources.ReloadableConfigSource;

class ReloadableConfigSourceTest {

    @Test
    void shouldReloadPropertiesWhenFileChanges() throws Exception {
        final Path file = Files.createTempFile("ether-config", ".properties");
        Files.writeString(file, "host=before\n");

        try (ReloadableConfigSource source = ReloadableConfigSource.of(file,
                ReloadableConfigSource.Format.PROPERTIES)) {
            assertEquals("before", source.get("host").orElseThrow());
            Files.writeString(file, "host=after\n");
            awaitValue(source, "host", "after");
        }
    }

    private static void awaitValue(final ReloadableConfigSource source, final String key, final String expected)
            throws InterruptedException {
        final long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
        while (System.nanoTime() < deadline) {
            if (expected.equals(source.get(key).orElse(null))) {
                return;
            }
            Thread.sleep(50L);
        }
        assertEquals(expected, source.get(key).orElse(null));
    }
}
