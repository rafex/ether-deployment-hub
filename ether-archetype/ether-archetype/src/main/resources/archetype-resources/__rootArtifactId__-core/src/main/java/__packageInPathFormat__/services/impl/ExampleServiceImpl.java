package ${package}.services.impl;

import ${package}.repository.ExampleRepository;
import ${package}.repository.ExampleRepository.ExampleEntity;
import ${package}.services.ExampleService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of {@link ExampleService}.
 * <p>
 * Depends only on the port interface {@link ExampleRepository}.
 * No framework or infrastructure code here.
 * </p>
 */
public final class ExampleServiceImpl implements ExampleService {

    private final ExampleRepository repository;

    public ExampleServiceImpl(final ExampleRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public Optional<ExampleEntity> findById(final Long id) {
        return repository.findById(id);
    }

    @Override
    public List<ExampleEntity> findAll() {
        return repository.findAll();
    }

    @Override
    public ExampleEntity create(final String name) {
        Objects.requireNonNull(name, "name");
        return repository.save(new ExampleEntity(null, name));
    }

    @Override
    public void deleteById(final Long id) {
        repository.deleteById(id);
    }
}
