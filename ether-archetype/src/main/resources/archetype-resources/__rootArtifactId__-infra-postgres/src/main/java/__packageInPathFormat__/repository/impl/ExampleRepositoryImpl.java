package ${package}.repository.impl;

import ${package}.repository.ExampleRepository;

import dev.rafex.ether.database.core.DatabaseClient;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * PostgreSQL adapter that implements {@link ExampleRepository}.
 * <p>
 * This class belongs to the infrastructure layer and must NOT depend on
 * transport adapters, bootstrap, or core service implementations.
 * </p>
 */
public final class ExampleRepositoryImpl implements ExampleRepository {

    private final DatabaseClient db;

    public ExampleRepositoryImpl(final DatabaseClient db) {
        this.db = Objects.requireNonNull(db, "db");
    }

    @Override
    public Optional<ExampleEntity> findById(final Long id) {
        // TODO: implement with db.query(...)
        return Optional.empty();
    }

    @Override
    public List<ExampleEntity> findAll() {
        // TODO: implement with db.queryList(...)
        return List.of();
    }

    @Override
    public ExampleEntity save(final ExampleEntity entity) {
        // TODO: implement insert/update with db.update(...)
        return entity;
    }

    @Override
    public void deleteById(final Long id) {
        // TODO: implement with db.update(...)
    }
}
