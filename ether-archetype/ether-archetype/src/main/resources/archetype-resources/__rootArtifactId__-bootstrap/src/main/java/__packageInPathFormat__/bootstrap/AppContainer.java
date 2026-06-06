package ${package}.bootstrap;

import ${package}.config.AppConfig;
import ${package}.db.Db;
import ${package}.repository.ExampleRepository;
import ${package}.repository.impl.ExampleRepositoryImpl;
import ${package}.services.ExampleService;
import ${package}.services.impl.ExampleServiceImpl;

import dev.rafex.ether.database.core.DatabaseClient;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import javax.sql.DataSource;

/**
 * Manual dependency injection container.
 * <p>
 * Centralizes wiring of all application components.
 * Supports overrides for testing.
 * Components are lazily initialized on first access.
 * </p>
 */
public final class AppContainer {

    public record Overrides(
            Optional<Supplier<AppConfig>> config,
            Optional<Supplier<DataSource>> dataSource,
            Optional<Supplier<ExampleRepository>> exampleRepository,
            Optional<Supplier<ExampleService>> exampleService) {

        public Overrides {
            config = config != null ? config : Optional.empty();
            dataSource = dataSource != null ? dataSource : Optional.empty();
            exampleRepository = exampleRepository != null ? exampleRepository : Optional.empty();
            exampleService = exampleService != null ? exampleService : Optional.empty();
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private Supplier<AppConfig> config;
            private Supplier<DataSource> dataSource;
            private Supplier<ExampleRepository> exampleRepository;
            private Supplier<ExampleService> exampleService;

            public Builder config(final Supplier<AppConfig> v) {
                config = v;
                return this;
            }

            public Builder dataSource(final Supplier<DataSource> v) {
                dataSource = v;
                return this;
            }

            public Builder exampleRepository(final Supplier<ExampleRepository> v) {
                exampleRepository = v;
                return this;
            }

            public Builder exampleService(final Supplier<ExampleService> v) {
                exampleService = v;
                return this;
            }

            public Overrides build() {
                return new Overrides(
                    Optional.ofNullable(config),
                    Optional.ofNullable(dataSource),
                    Optional.ofNullable(exampleRepository),
                    Optional.ofNullable(exampleService)
                );
            }
        }
    }

    private final Lazy<AppConfig> config;
    private final Lazy<DataSource> dataSource;
    private final Lazy<DatabaseClient> databaseClient;
    private final Lazy<ExampleRepository> exampleRepository;
    private final Lazy<ExampleService> exampleService;

    public AppContainer() {
        this(Overrides.builder().build());
    }

    public AppContainer(final Overrides overrides) {
        Objects.requireNonNull(overrides, "overrides");

        config = new Lazy<>(select(overrides.config(), AppConfig::load));
        dataSource = new Lazy<>(select(overrides.dataSource(), () -> Db.dataSource()));
        databaseClient = new Lazy<>(() -> Db.databaseClient());

        exampleRepository = new Lazy<>(
            select(overrides.exampleRepository(), () -> new ExampleRepositoryImpl(databaseClient())));
        exampleService = new Lazy<>(
            select(overrides.exampleService(), () -> new ExampleServiceImpl(exampleRepository())));
    }

    public AppConfig config() {
        return config.get();
    }

    public DataSource dataSource() {
        return dataSource.get();
    }

    public DatabaseClient databaseClient() {
        return databaseClient.get();
    }

    public ExampleRepository exampleRepository() {
        return exampleRepository.get();
    }

    public ExampleService exampleService() {
        return exampleService.get();
    }

    /**
     * Eager-initializes all components to catch config/DB errors at startup.
     */
    public void warmup() {
        config();
        dataSource();
        databaseClient();
        exampleRepository();
        exampleService();
    }

    private static <T> Supplier<T> select(final Optional<Supplier<T>> override, final Supplier<T> def) {
        return override.orElse(def);
    }
}
