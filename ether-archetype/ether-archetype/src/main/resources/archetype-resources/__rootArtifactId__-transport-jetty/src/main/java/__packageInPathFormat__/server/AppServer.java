package ${package}.server;

import ${package}.bootstrap.AppContainer;
import ${package}.config.AppConfig;
import ${package}.handlers.HealthHandler;

import dev.rafex.ether.http.jetty12.EtherServer;

import java.util.logging.Logger;

/**
 * Configures and starts the embedded Jetty HTTP server.
 * <p>
 * Registers all HTTP handlers and applies middleware.
 * Depends on {@link AppContainer} to resolve service instances.
 * </p>
 */
public final class AppServer {

    private static final Logger LOG = Logger.getLogger(AppServer.class.getName());

    private AppServer() {
    }

    public static void start(final AppContainer container) throws Exception {
        final var config = AppConfig.load().server();
        final var port = config.port();

        final var server = EtherServer.builder()
            .port(port)
            // Register handlers
            .handler("/health", new HealthHandler())
            // Add more handlers here:
            // .handler("/api/examples", new ExampleHandler(container.exampleService()))
            .build();

        LOG.info("Server starting on port " + port);
        server.start();
        server.join();
    }
}
