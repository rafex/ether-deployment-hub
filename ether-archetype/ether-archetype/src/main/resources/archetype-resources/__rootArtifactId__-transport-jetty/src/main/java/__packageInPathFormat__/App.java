package ${package};

import ${package}.bootstrap.AppBootstrap;
import ${package}.config.AppConfig;
import ${package}.server.AppServer;

import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application entry point.
 */
public final class App {

    private static final Logger LOG = Logger.getLogger(App.class.getName());

    private App() {
    }

    public static void main(final String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        configureLogging(args);

        LOG.info("Starting ${rootArtifactId}...");

        try (var runtime = AppBootstrap.start()) {
            final var container = runtime.container();
            AppServer.start(container);
        }
    }

    private static void configureLogging(final String[] args) {
        var levelStr = AppConfig.load().server().sandbox() ? "DEBUG" : "INFO";

        for (final String arg : args) {
            if (arg != null && arg.startsWith("--log=")) {
                levelStr = arg.substring("--log=".length());
                break;
            }
        }

        final var level = parseLevel(levelStr, Level.INFO);
        final var root = Logger.getLogger("");
        root.setLevel(level);
        for (final var handler : root.getHandlers()) {
            handler.setLevel(level);
        }
        LOG.info("Log level set to " + level.getName());
    }

    private static Level parseLevel(final String levelStr, final Level fallback) {
        if (levelStr == null || levelStr.isBlank()) {
            return fallback;
        }
        return switch (levelStr.toUpperCase(Locale.ROOT)) {
            case "DEBUG", "FINE" -> Level.FINE;
            case "INFO" -> Level.INFO;
            case "WARN", "WARNING" -> Level.WARNING;
            case "ERROR", "SEVERE" -> Level.SEVERE;
            default -> fallback;
        };
    }
}
