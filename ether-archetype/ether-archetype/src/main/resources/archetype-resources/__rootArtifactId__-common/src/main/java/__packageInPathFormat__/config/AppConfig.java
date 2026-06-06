package ${package}.config;

import dev.rafex.ether.config.EtherConfig;
import dev.rafex.ether.config.sources.EnvironmentConfigSource;
import dev.rafex.ether.config.sources.SystemPropertyConfigSource;

import java.util.Objects;

/**
 * Centralized configuration for ${rootArtifactId}.
 * <p>
 * Loads configuration from multiple sources in priority order:
 * <ol>
 * <li>Environment variables (highest priority)</li>
 * <li>System properties</li>
 * </ol>
 */
public record AppConfig(DatabaseConfig database, ServerConfig server) {

    public AppConfig {
        Objects.requireNonNull(database, "database cannot be null");
        Objects.requireNonNull(server, "server cannot be null");
    }

    private static volatile AppConfig INSTANCE;

    /**
     * Load configuration from default sources (environment variables + system properties).
     * Returns a singleton instance.
     */
    public static AppConfig load() {
        if (INSTANCE == null) {
            synchronized (AppConfig.class) {
                if (INSTANCE == null) {
                    INSTANCE = loadFrom(EtherConfig.of(
                        new EnvironmentConfigSource(),
                        new SystemPropertyConfigSource()
                    ));
                }
            }
        }
        return INSTANCE;
    }

    public static AppConfig loadFrom(final EtherConfig config) {
        final var database = DatabaseConfig.from(config);
        final var server = ServerConfig.from(config);
        return new AppConfig(database, server);
    }

    /** Clears the cached instance — useful in tests. */
    public static void reset() {
        synchronized (AppConfig.class) {
            INSTANCE = null;
        }
    }
}
