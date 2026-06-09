package ${package}.bootstrap;

import java.util.Objects;

/**
 * Builds a runtime with container + closer.
 * <p>
 * Usage:
 * <pre>{@code
 * try (var runtime = AppBootstrap.start()) {
 *     var container = runtime.container();
 *     // start server, pass container
 * }
 * }</pre>
 * </p>
 */
public final class AppBootstrap {

    private AppBootstrap() {
    }

    public record AppRuntime(AppContainer container, Closer closer) implements AutoCloseable {

        public AppRuntime {
            Objects.requireNonNull(container, "container");
            Objects.requireNonNull(closer, "closer");
        }

        @Override
        public void close() {
            closer.close();
        }
    }

    public static AppRuntime start() {
        return start(new AppContainer(), true);
    }

    public static AppRuntime start(final AppContainer container, final boolean warmup) {
        Objects.requireNonNull(container, "container");

        final var closer = new Closer();

        if (warmup) {
            container.warmup();
        }

        final var ds = container.dataSource();
        if (ds instanceof final AutoCloseable ac) {
            closer.register(ac);
        }

        final var rt = new AppRuntime(container, closer);
        Runtime.getRuntime().addShutdownHook(new Thread(rt::close, "app-shutdown"));
        return rt;
    }
}
