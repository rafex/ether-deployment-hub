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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.rafex.ether.config.annotations.ConfigAlias;
import dev.rafex.ether.config.annotations.ConfigPrefix;
import dev.rafex.ether.config.exceptions.ConfigValidationException;
import dev.rafex.ether.config.sources.EnvironmentConfigSource;
import dev.rafex.ether.config.sources.MapConfigSource;
import dev.rafex.ether.config.sources.PropertiesFileConfigSource;
import dev.rafex.ether.config.validation.Min;
import dev.rafex.ether.config.validation.NotBlank;
import dev.rafex.ether.config.validation.Pattern;
import dev.rafex.ether.config.validation.Required;
import dev.rafex.ether.config.validation.Size;
import dev.rafex.ether.config.validation.Valid;

class ConfigBinderTest {

    @ConfigPrefix("server")
    record NamespacedServerConfig(int port, String host) {
    }

    record LegacyHttpConfig(@ConfigAlias("http.port") int port, @ConfigAlias("http.pool.name") String poolName) {
    }

    record ServiceConfig(String host, int port, boolean secure, Duration timeout, URI endpoint) {
    }

    record DatabaseConfig(@NotBlank String host, @Min(1) int port) {
    }

    record ServerConfig(@Required @NotBlank String host, @Pattern("^https?://.+") String endpoint) {
    }

    record ApplicationConfig(@Valid DatabaseConfig database, List<String> allowedOrigins, Map<String, String> labels,
            @Size(min = 1) @Valid List<ServerConfig> upstreams) {
    }

    @Test
    void shouldBindRecordFromLayeredSources() {
        final var config = EtherConfig.of(new MapConfigSource("runtime", Map.of("host", "api.example.com", "port",
                "443", "secure", "true", "timeout", "PT5S", "endpoint", "https://api.example.com/v1")));

        final var bound = config.bind(ServiceConfig.class);
        assertEquals("api.example.com", bound.host());
        assertEquals(443, bound.port());
        assertEquals(true, bound.secure());
        assertEquals(Duration.ofSeconds(5), bound.timeout());
        assertEquals(URI.create("https://api.example.com/v1"), bound.endpoint());
    }

    @Test
    void shouldReadPropertiesFileSource() throws Exception {
        final var file = Files.createTempFile("ether-config", ".properties");
        Files.writeString(file, "host=localhost\nport=8080\n");

        final var source = new PropertiesFileConfigSource(file);
        assertEquals("localhost", source.get("host").orElseThrow());
        assertEquals("8080", source.get("port").orElseThrow());
    }

    @Test
    void shouldBindNestedRecordsListsAndMaps() {
        final var config = EtherConfig.of(new MapConfigSource("runtime",
                Map.ofEntries(Map.entry("database.host", "db.internal"), Map.entry("database.port", "5432"),
                        Map.entry("allowedOrigins[0]", "https://a.example.com"),
                        Map.entry("allowedOrigins[1]", "https://b.example.com"), Map.entry("labels.region", "mx"),
                        Map.entry("labels.tier", "prod"), Map.entry("upstreams[0].host", "edge-a"),
                        Map.entry("upstreams[0].endpoint", "https://edge-a.example.com"),
                        Map.entry("upstreams[1].host", "edge-b"),
                        Map.entry("upstreams[1].endpoint", "https://edge-b.example.com"))));

        final var bound = config.bindValidated(ApplicationConfig.class);
        assertEquals("db.internal", bound.database().host());
        assertEquals(5432, bound.database().port());
        assertEquals(List.of("https://a.example.com", "https://b.example.com"), bound.allowedOrigins());
        assertEquals(Map.of("region", "mx", "tier", "prod"), bound.labels());
        assertEquals(2, bound.upstreams().size());
        assertEquals("edge-a", bound.upstreams().get(0).host());
    }

    @Test
    void shouldRejectInvalidValidatedConfig() {
        final var config = EtherConfig.of(new MapConfigSource("runtime",
                Map.ofEntries(Map.entry("database.host", ""), Map.entry("database.port", "0"),
                        Map.entry("upstreams[0].host", ""), Map.entry("upstreams[0].endpoint", "ftp://bad"))));

        assertThrows(ConfigValidationException.class, () -> config.bindValidated(ApplicationConfig.class));
    }

    @Test
    void shouldBindNamespacedRecordByAnnotationAndPrefix() {
        final var config = EtherConfig.of(new MapConfigSource("runtime", Map.of("server.port", "9090", "server.host",
                "127.0.0.1", "http.port", "8080", "http.pool.name", "ether-http")));

        final var server = config.bind(NamespacedServerConfig.class);
        final var legacy = config.bind(LegacyHttpConfig.class);

        assertEquals(9090, server.port());
        assertEquals("127.0.0.1", server.host());
        assertEquals(8080, legacy.port());
        assertEquals("ether-http", legacy.poolName());
    }

    @Test
    void shouldResolveDotNotationFromEnvironmentVariables() {
        final var config = EtherConfig
                .of(new EnvironmentConfigSource(Map.of("SERVER_PORT", "9191", "SERVER_HOST", "0.0.0.0")));

        final var server = config.bind(NamespacedServerConfig.class);
        assertEquals(9191, server.port());
        assertEquals("0.0.0.0", server.host());
    }
}
