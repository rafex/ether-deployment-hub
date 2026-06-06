package ${package}.db;

import ${package}.config.AppConfig;
import ${package}.config.DatabaseConfig;

import dev.rafex.ether.database.core.DatabaseClient;
import dev.rafex.ether.jdbc.client.JdbcDatabaseClient;

import java.util.logging.Logger;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Singleton DataSource and DatabaseClient factory.
 * <p>
 * Call {@link #init(DatabaseConfig)} once at startup (from bootstrap),
 * then use {@link #dataSource()} and {@link #databaseClient()} anywhere.
 * </p>
 */
public final class Db {

    private static final Logger LOG = Logger.getLogger(Db.class.getName());

    private static volatile HikariDataSource DS;
    private static volatile DatabaseClient CLIENT;

    private Db() {
    }

    /**
     * Initializes the connection pool with the given configuration.
     * Must be called before {@link #dataSource()} or {@link #databaseClient()}.
     */
    public static synchronized void init(final DatabaseConfig config) {
        if (DS != null) {
            LOG.warning("Database already initialized, ignoring re-initialization");
            return;
        }
        DS = createDataSource(config);
        CLIENT = new JdbcDatabaseClient(DS);
        LOG.info("Database initialized: " + config.maskedUrl());
    }

    /** Returns the shared DataSource, initializing from AppConfig if needed. */
    public static DataSource dataSource() {
        ensureInitialized();
        return DS;
    }

    /** Returns the shared DatabaseClient, initializing from AppConfig if needed. */
    public static DatabaseClient databaseClient() {
        ensureInitialized();
        return CLIENT;
    }

    private static synchronized void ensureInitialized() {
        if (DS == null) {
            init(AppConfig.load().database());
        }
    }

    private static HikariDataSource createDataSource(final DatabaseConfig cfg) {
        final var hikari = new HikariConfig();
        hikari.setJdbcUrl(cfg.url());
        if (cfg.user() != null && !cfg.user().isBlank()) {
            hikari.setUsername(cfg.user());
        }
        if (cfg.password() != null && !cfg.password().isBlank()) {
            hikari.setPassword(cfg.password());
        }
        hikari.setMaximumPoolSize(cfg.maxPoolSize());
        hikari.setMinimumIdle(cfg.minIdle());
        hikari.setConnectionTimeout(cfg.connectionTimeoutMs());
        hikari.setIdleTimeout(cfg.idleTimeoutMs());
        hikari.setMaxLifetime(cfg.maxLifetimeMs());
        hikari.setValidationTimeout(cfg.validationTimeoutMs());
        hikari.setPoolName("app-pool");
        hikari.addDataSourceProperty("preparedStatementCacheQueries", "256");
        hikari.addDataSourceProperty("preparedStatementCacheSizeMiB", "5");
        return new HikariDataSource(hikari);
    }
}
