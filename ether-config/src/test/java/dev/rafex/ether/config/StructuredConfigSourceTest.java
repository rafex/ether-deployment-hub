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

import org.junit.jupiter.api.Test;

import dev.rafex.ether.config.sources.JsonFileConfigSource;
import dev.rafex.ether.config.sources.TomlFileConfigSource;
import dev.rafex.ether.config.sources.YamlFileConfigSource;

class StructuredConfigSourceTest {

    @Test
    void shouldLoadJsonYamlAndToml() throws Exception {
        final var json = Files.createTempFile("ether-config", ".json");
        Files.writeString(json, """
                {"server":{"host":"json.local","ports":[8080,8081]}}
                """);
        assertEquals("json.local", new JsonFileConfigSource(json).get("server.host").orElseThrow());
        assertEquals("8081", new JsonFileConfigSource(json).get("server.ports[1]").orElseThrow());

        final var yaml = Files.createTempFile("ether-config", ".yaml");
        Files.writeString(yaml, """
                server:
                  host: yaml.local
                  ports:
                    - 8080
                    - 8081
                """);
        assertEquals("yaml.local", new YamlFileConfigSource(yaml).get("server.host").orElseThrow());
        assertEquals("8080", new YamlFileConfigSource(yaml).get("server.ports[0]").orElseThrow());

        final var toml = Files.createTempFile("ether-config", ".toml");
        Files.writeString(toml, """
                [server]
                host = "toml.local"
                ports = [8080, 8081]
                """);
        assertEquals("toml.local", new TomlFileConfigSource(toml).get("server.host").orElseThrow());
        assertEquals("8081", new TomlFileConfigSource(toml).get("server.ports[1]").orElseThrow());
    }
}
