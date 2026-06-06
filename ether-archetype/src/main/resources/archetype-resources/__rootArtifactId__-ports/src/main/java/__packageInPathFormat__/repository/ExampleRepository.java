package ${package}.repository;

import java.util.List;
import java.util.Optional;

/**
 * Example port (repository interface) for the hexagonal architecture.
 * <p>
 * Ports define the boundary between the domain (core) and the outside world.
 * Infrastructure adapters implement this interface; core services depend on it.
 * </p>
 * <p>
 * Rename this interface and add your domain-specific methods.
 * </p>
 */
public interface ExampleRepository {

    Optional<ExampleEntity> findById(Long id);

    List<ExampleEntity> findAll();

    ExampleEntity save(ExampleEntity entity);

    void deleteById(Long id);

    /**
     * Simple placeholder entity. Move to a dedicated file when expanding.
     */
    record ExampleEntity(Long id, String name) {
    }
}
