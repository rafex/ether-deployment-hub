package ${package}.config;

import dev.rafex.ether.config.EtherConfig;

/**
 * Database configuration.
 * <p>
 * Reads values from environment variables or system properties.
 * Defaults are suitable for local development with Docker.
 * </p>
 */
public record DatabaseConfig(
        String url,
        String user,
        String password,
        int maxPoolSize,
        int minIdle,
        long connectionTimeoutMs,
        long idleTimeoutMs,
        long maxLifetimeMs,
        long validationTimeoutMs) {

    public static DatabaseConfig from(final EtherConfig config) {
        return new DatabaseConfig(
            config.get("DB_URL", "jdbc:postgresql://localhost:5432/appdb"),
            config.get("DB_USER", "app"),
            config.get("DB_PASSWORD", ""),
            config.getInt("DB_MAX_POOL_SIZE", 10),
            config.getInt("DB_MIN_IDLE", 2),
            config.getLong("DB_CONNECTION_TIMEOUT_MS", 30_000L),
            config.getLong("DB_IDLE_TIMEOUT_MS", 600_000L),
            config.getLong("DB_MAX_LIFETIME_MS", 1_800_000L),
            config.getLong("DB_VALIDATION_TIMEOUT_MS", 5_000L)
        );
    }

    /** Returns a masked URL safe for logging. */
    public String maskedUrl() {
        if (url == null || url.isBlank()) {
            return "<not-set>";
        }
        try {
            // Extract host:port/db without credentials
            final var stripped = url.startsWith("jdbc:") ? url.substring(5) : url;
            final var uri = new java.net.URI(stripped);
            final var sb = new StringBuilder();
            if (uri.getHost() != null) {
                sb.append(uri.getHost());
            }
            if (uri.getPort() != -1) {
                sb.append(":").append(uri.getPort());
            }
            if (uri.getPath() != null && !uri.getPath().isBlank()) {
                sb.append(uri.getPath());
            }
            return sb.isEmpty() ? "<masked>" : sb.toString();
        } catch (final Exception e) {
            return "<masked>";
        }
    }
}
