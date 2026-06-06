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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class ReloadableConfigSource implements ConfigSource, AutoCloseable {

    public enum Format {
        PROPERTIES, JSON, YAML, TOML
    }

    private final String name;
    private final Path path;
    private final Format format;
    private final AtomicReference<Map<String, String>> values;
    private final AtomicBoolean running;
    private final WatchService watchService;
    private final Thread watcherThread;

    private ReloadableConfigSource(final Path path, final Format format) throws IOException {
        this.path = Objects.requireNonNull(path, "path");
        this.format = Objects.requireNonNull(format, "format");
        name = "reloadable:" + format.name().toLowerCase() + ":" + path.toAbsolutePath();
        values = new AtomicReference<>(load(path, format));
        running = new AtomicBoolean(true);
        watchService = path.getParent().getFileSystem().newWatchService();
        path.getParent().register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY);
        watcherThread = Thread.ofVirtual().name("ether-config-reload-" + path.getFileName()).start(this::watchLoop);
    }

    public static ReloadableConfigSource of(final Path path, final Format format) throws IOException {
        return new ReloadableConfigSource(path, format);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Optional<String> get(final String key) {
        return Optional.ofNullable(values.get().get(key));
    }

    @Override
    public Map<String, String> entries() {
        return values.get();
    }

    public void reloadNow() throws IOException {
        values.set(load(path, format));
    }

    @Override
    public void close() throws IOException {
        running.set(false);
        watcherThread.interrupt();
        watchService.close();
    }

    private void watchLoop() {
        while (running.get()) {
            try {
                final var key = watchService.take();
                for (final WatchEvent<?> event : key.pollEvents()) {
                    final var changed = (Path) event.context();
                    if (path.getFileName().equals(changed)) {
                        try {
                            reloadNow();
                        } catch (IOException _) {
                            // Keep serving the previous snapshot until a valid reload is available.
                        }
                    }
                }
                key.reset();
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static Map<String, String> load(final Path path, final Format format) throws IOException {
        return switch (format) {
        case PROPERTIES -> new PropertiesFileConfigSource(path).entries();
        case JSON -> StructuredConfigSource.loadJson(path);
        case YAML -> StructuredConfigSource.loadYaml(path);
        case TOML -> StructuredConfigSource.loadToml(path);
        };
    }
}
