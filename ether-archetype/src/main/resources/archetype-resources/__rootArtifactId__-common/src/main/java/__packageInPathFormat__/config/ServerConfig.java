package ${package}.config;

import dev.rafex.ether.config.EtherConfig;

/**
 * HTTP server configuration.
 */
public record ServerConfig(int port, boolean sandbox) {

    public static ServerConfig from(final EtherConfig config) {
        return new ServerConfig(
            config.getInt("PORT", 8080),
            config.getBoolean("SANDBOX", false)
        );
    }
}
