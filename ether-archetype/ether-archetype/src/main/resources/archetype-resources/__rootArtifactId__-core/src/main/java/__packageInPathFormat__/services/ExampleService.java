package ${package}.services;

import ${package}.repository.ExampleRepository.ExampleEntity;

import java.util.List;
import java.util.Optional;

/**
 * Port (use-case interface) for example domain operations.
 * <p>
 * Services are defined here in core and implemented in the same package (impl/).
 * Transport adapters (Jetty, gRPC, etc.) depend on this interface, never on
 * the implementation.
 * </p>
 */
public interface ExampleService {

    Optional<ExampleEntity> findById(Long id);

    List<ExampleEntity> findAll();

    ExampleEntity create(String name);

    void deleteById(Long id);
}
